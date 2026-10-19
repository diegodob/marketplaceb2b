package com.marketplaceb2b.service.pricing;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.marketplaceb2b.dto.DiscountOrigin;
import com.marketplaceb2b.model.DiscountTargetType;
import com.marketplaceb2b.model.Money;
import com.marketplaceb2b.model.ScaleDiscount;
import com.marketplaceb2b.model.ScaleTier;

/**
 * The scale lever. A scale applies when the available units of its target reach its first tier; the
 * reached tier (the greatest {@code from_qty} not above those units) percentage applies to all the
 * target's available units. A SKU scale covers one product; a FAMILY scale covers the cart lines whose
 * product belongs to the family and reaches its tier on their combined units. The covered units are
 * consumed and the saving is reported as a scale entry.
 */
@Component
public class ScaleDiscountStrategy implements LeverStrategy {

    @Override
    public List<ScheduledLever> applicable(PricingContext context) {
        List<ScheduledLever> applicable = new ArrayList<>();
        for (ScaleDiscount scale : context.scales()) {
            if (reachedTier(scale, targetUnits(scale, context)) != null) {
                applicable.add(new ScheduledLever(this, scale.id(), scale.priority(), LeverType.SCALE,
                        targetKind(scale)));
            }
        }
        return applicable;
    }

    @Override
    public void apply(PricingContext context, long sourceId) {
        ScaleDiscount scale = context.scale(sourceId);
        if (scale == null) {
            return;
        }
        List<PricingLine> covered = coveredLines(scale, context);
        ScaleTier tier = reachedTier(scale, totalAvailable(covered));
        if (tier == null) {
            return;
        }

        Money totalDiscount = Money.zero(context.ruleset());
        for (PricingLine line : covered) {
            int units = line.availableUnits();
            if (units == 0) {
                continue;
            }
            Money lineDiscount = line.unitPriceMoney().times(units).percentage(tier.percentage());
            line.discountBy(lineDiscount);
            line.consume(units);
            totalDiscount = totalDiscount.plus(lineDiscount);
        }
        context.recordDiscount(DiscountOrigin.SCALE, scale.id(), detail(scale, tier), totalDiscount);
    }

    private static List<PricingLine> coveredLines(ScaleDiscount scale, PricingContext context) {
        List<PricingLine> covered = new ArrayList<>();
        for (PricingLine line : context.lines()) {
            if (!line.gift() && matches(scale, line, context)) {
                covered.add(line);
            }
        }
        return covered;
    }

    private static boolean matches(ScaleDiscount scale, PricingLine line, PricingContext context) {
        if (scale.targetType() == DiscountTargetType.SKU) {
            return scale.productId() != null && scale.productId() == line.productId();
        }
        Long familyId = context.familyOf(line.productId());
        return familyId != null && familyId.equals(scale.familyId());
    }

    private static int targetUnits(ScaleDiscount scale, PricingContext context) {
        return totalAvailable(coveredLines(scale, context));
    }

    private static int totalAvailable(List<PricingLine> lines) {
        int units = 0;
        for (PricingLine line : lines) {
            units += line.availableUnits();
        }
        return units;
    }

    private static ScaleTier reachedTier(ScaleDiscount scale, int units) {
        ScaleTier reached = null;
        for (ScaleTier tier : scale.tiers()) {
            if (tier.fromQty() <= units) {
                reached = tier;
            }
        }
        return reached;
    }

    private static TargetKind targetKind(ScaleDiscount scale) {
        return scale.targetType() == DiscountTargetType.SKU ? TargetKind.SKU : TargetKind.FAMILY;
    }

    private static String detail(ScaleDiscount scale, ScaleTier tier) {
        String target = scale.targetType() == DiscountTargetType.SKU ? "producto" : "familia";
        return "Escala " + target + " " + tier.fromQty() + "+ (" + tier.percentage().stripTrailingZeros() + "%)";
    }
}
