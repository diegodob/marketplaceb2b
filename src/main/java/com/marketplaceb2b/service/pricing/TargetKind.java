package com.marketplaceb2b.service.pricing;

/** A lever's target kind, in the order a tie on priority resolves (a SKU is more specific than a family). */
public enum TargetKind {
    NONE,
    SKU,
    FAMILY
}
