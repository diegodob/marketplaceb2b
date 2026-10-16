package com.marketplaceb2b.service.pricing;

import java.util.List;

/**
 * A strategy that applies one commercial lever type (combo, and later scale and gift). The pipeline
 * schedules the levers a strategy reports as applicable and then asks the strategy to apply each one,
 * so the order is global across types and a lever never discounts units another lever already consumed.
 */
public interface LeverStrategy {

    /**
     * The levers of this strategy's type that currently apply to the cart, each with the scheduling key
     * (its id and priority). Applicability is re-checked when the lever is applied, because an earlier
     * lever may have consumed the units it relied on.
     */
    List<ScheduledLever> applicable(PricingContext context);

    /** Applies the lever with the given id. A no-op if it no longer applies. */
    void apply(PricingContext context, long sourceId);
}
