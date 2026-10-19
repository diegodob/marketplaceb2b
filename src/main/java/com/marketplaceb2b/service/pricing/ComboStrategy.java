package com.marketplaceb2b.service.pricing;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.marketplaceb2b.dto.DiscountOrigin;
import com.marketplaceb2b.model.Combo;
import com.marketplaceb2b.model.ComboItem;
import com.marketplaceb2b.model.Money;
import com.marketplaceb2b.model.Ruleset;

/**
 * The combo lever. A combo applies when every item still has enough available units to reach its
 * minimum. It covers exactly the minimum quantity per item at the combo price: each covered line keeps
 * its list gross and gets a discount equal to its share of {@code covered list value - combo price},
 * prorated by the covered line's share of the covered list value (the last line takes the rounding
 * remainder, so the covered bases add up to the combo price). Units above the minimum keep the regular
 * price, and the covered units are consumed.
 */
@Component
public class ComboStrategy implements LeverStrategy {

    @Override
    public List<ScheduledLever> applicable(PricingContext context) {
        List<ScheduledLever> applicable = new ArrayList<>();
        for (Combo combo : context.combos()) {
            if (applies(combo, context)) {
                applicable.add(new ScheduledLever(this, combo.id(), combo.priority(), LeverType.COMBO,
                        TargetKind.NONE));
            }
        }
        return applicable;
    }

    @Override
    public void apply(PricingContext context, long sourceId) {
        Combo combo = context.combo(sourceId);
        if (combo == null || !applies(combo, context)) {
            return;
        }
        apply(combo, context);
    }

    private boolean applies(Combo combo, PricingContext context) {
        for (ComboItem item : combo.items()) {
            if (context.availableUnits(item.productId()) < item.minQty()) {
                return false;
            }
        }
        return true;
    }

    private void apply(Combo combo, PricingContext context) {
        List<ComboItem> items = combo.items();

        Money totalListValue = Money.zero(context.ruleset());
        for (ComboItem item : items) {
            totalListValue = totalListValue.plus(coveredValue(item, context));
        }
        Money comboPrice = Money.of(combo.comboPrice(), context.ruleset());

        Money allocated = Money.zero(context.ruleset());
        for (int index = 0; index < items.size(); index++) {
            ComboItem item = items.get(index);
            Money covered = coveredValue(item, context);
            Money base = index == items.size() - 1
                    ? comboPrice.minus(allocated)
                    : prorate(comboPrice, covered, totalListValue, context.ruleset());
            allocated = allocated.plus(base);

            Money lineDiscount = covered.minus(base);
            PricingLine line = context.line(item.productId());
            line.discountBy(lineDiscount);
            line.consume(item.minQty());
            context.recordDiscount(item.productId(), DiscountOrigin.COMBO, combo.id(), combo.name(), lineDiscount);
        }
    }

    private static Money coveredValue(ComboItem item, PricingContext context) {
        return context.line(item.productId()).unitPriceMoney().times(item.minQty());
    }

    private static Money prorate(Money comboPrice, Money covered, Money totalListValue, Ruleset ruleset) {
        if (totalListValue.amount().signum() == 0) {
            return Money.zero(ruleset.currencyScale(), ruleset.roundingMode());
        }
        BigDecimal ratio = covered.amount().divide(totalListValue.amount(), MathContext.DECIMAL128);
        return comboPrice.times(ratio);
    }
}
