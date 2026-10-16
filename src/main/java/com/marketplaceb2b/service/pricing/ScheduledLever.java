package com.marketplaceb2b.service.pricing;

/** A lever to apply, scheduled by the pipeline: its strategy, its id and its priority. */
public record ScheduledLever(LeverStrategy strategy, long sourceId, int priority) {
}
