package com.marketplaceb2b.service.pricing;

/** A lever to apply, scheduled by the pipeline with the keys that make the order deterministic. */
public record ScheduledLever(LeverStrategy strategy, long sourceId, int priority, LeverType type,
                             TargetKind target) {
}
