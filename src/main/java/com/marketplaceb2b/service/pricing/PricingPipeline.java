package com.marketplaceb2b.service.pricing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Applies the applicable levers in a fixed order: descending priority, ties broken by the lever id (and,
 * for full determinism, by the strategy class name). It never tries to minimize the total, and each
 * applied lever consumes the units it discounts, so a later lever never discounts the same units again.
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
                .thenComparingLong(ScheduledLever::sourceId)
                .thenComparing(lever -> lever.strategy().getClass().getName()));
        for (ScheduledLever lever : scheduled) {
            lever.strategy().apply(context, lever.sourceId());
        }
    }
}
