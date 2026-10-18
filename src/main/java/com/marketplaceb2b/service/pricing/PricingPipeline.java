package com.marketplaceb2b.service.pricing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Applies the applicable levers in a fixed, deterministic order: descending priority; on a tie,
 * combos before scales before gifts, a SKU scale before a FAMILY scale, then the newest lever (greater
 * id) first. It never tries to minimize the total, and each applied lever consumes the units it
 * discounts, so a later lever never discounts the same units again.
 */
@Component
public class PricingPipeline {

    private final List<LeverStrategy> strategies;

    public PricingPipeline(List<LeverStrategy> strategies) {
        this.strategies = List.copyOf(strategies);
    }

    public void run(PricingContext context) {
        List<ScheduledLever> scheduled = new ArrayList<>();
        for (LeverStrategy strategy : strategies) {
            scheduled.addAll(strategy.applicable(context));
        }
        scheduled.sort(Comparator
                .comparingInt(ScheduledLever::priority).reversed()
                .thenComparingInt(lever -> lever.type().ordinal())
                .thenComparingInt(lever -> lever.target().ordinal())
                .thenComparing(Comparator.comparingLong(ScheduledLever::sourceId).reversed()));
        for (ScheduledLever lever : scheduled) {
            lever.strategy().apply(context, lever.sourceId());
        }
    }
}
