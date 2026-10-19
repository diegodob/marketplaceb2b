package com.marketplaceb2b.service.pricing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.marketplaceb2b.dto.DiscountOrigin;
import com.marketplaceb2b.model.Gift;
import com.marketplaceb2b.model.GiftCondition;
import com.marketplaceb2b.model.Money;
import com.marketplaceb2b.model.ProductPrice;
import com.marketplaceb2b.service.exception.MissingPriceException;
import com.marketplaceb2b.service.exception.MissingTaxRateException;

/**
 * The gift lever. A gift applies when its condition is met on the gross cart — the original units and
 * list value, before the other levers. It adds a line for the gift product, marked as a gift, and a
 * GIFT discount equal to the line's value, so the net base is zero and no product is paid. A taxable
 * gift is taxed on its gross by the breakdown (the documented exception). The gift runs after the
 * other levers and never consumes cart units.
 */
@Component
public class GiftStrategy implements LeverStrategy {

    @Override
    public List<ScheduledLever> applicable(PricingContext context) {
        List<ScheduledLever> applicable = new ArrayList<>();
        for (Gift gift : context.gifts()) {
            if (conditionMet(gift.condition(), context)) {
                applicable.add(new ScheduledLever(this, gift.id(), gift.priority(), LeverType.GIFT,
                        TargetKind.NONE));
            }
        }
        return applicable;
    }

    @Override
    public void apply(PricingContext context, long sourceId) {
        Gift gift = context.gift(sourceId);
        if (gift == null || !conditionMet(gift.condition(), context)) {
            return;
        }
        ProductPrice price = context.prices().get(gift.giftProductId());
        if (price == null) {
            throw new MissingPriceException(context.ruleset().id(), gift.giftProductId());
        }
        BigDecimal rate = context.rates().get(price.taxCategoryId());
        if (rate == null) {
            throw new MissingTaxRateException(context.ruleset().id(), price.taxCategoryId());
        }

        PricingLine line = PricingLine.giftLine(gift.giftProductId(), gift.giftQty(), price.unitPrice(), rate,
                gift.taxable(), context.ruleset());
        Money value = line.gross();
        line.discountBy(value);
        context.recordDiscount(gift.giftProductId(), DiscountOrigin.GIFT, gift.id(), detail(gift), value);
        context.addGiftLine(line);
    }

    private static boolean conditionMet(GiftCondition condition, PricingContext context) {
        if (condition.minQty() != null && targetUnits(condition, context) < condition.minQty()) {
            return false;
        }
        return condition.minAmount() == null || grossSubtotal(context).compareTo(condition.minAmount()) >= 0;
    }

    private static int targetUnits(GiftCondition condition, PricingContext context) {
        int units = 0;
        for (PricingLine line : context.lines()) {
            if (!line.gift() && matches(condition, line, context)) {
                units += line.qty();
            }
        }
        return units;
    }

    private static boolean matches(GiftCondition condition, PricingLine line, PricingContext context) {
        if (condition.productId() != null) {
            return condition.productId() == line.productId();
        }
        Long familyId = context.familyOf(line.productId());
        return condition.familyId() != null && condition.familyId().equals(familyId);
    }

    private static BigDecimal grossSubtotal(PricingContext context) {
        Money gross = Money.zero(context.ruleset());
        for (PricingLine line : context.lines()) {
            if (!line.gift()) {
                gross = gross.plus(line.gross());
            }
        }
        return gross.amount();
    }

    private static String detail(Gift gift) {
        return "Obsequio x" + gift.giftQty();
    }
}
