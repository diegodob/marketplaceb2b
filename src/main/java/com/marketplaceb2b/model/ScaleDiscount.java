package com.marketplaceb2b.model;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ScaleDiscount(
        Long id,
        @NotNull Long rulesetId,
        @NotNull DiscountTargetType targetType,
        Long productId,
        Long familyId,
        @NotNull @Size(min = 1) @Valid List<@NotNull ScaleTier> tiers) {

    @AssertTrue(message = "target must be exactly one of productId (SKU) or familyId (FAMILY)")
    public boolean isTargetConsistent() {
        if (targetType == DiscountTargetType.SKU) {
            return productId != null && familyId == null;
        }
        if (targetType == DiscountTargetType.FAMILY) {
            return familyId != null && productId == null;
        }
        return false;
    }

    @AssertTrue(message = "tiers must be strictly increasing by starting quantity")
    public boolean isTiersIncreasing() {
        if (tiers == null) {
            return true;
        }
        int previous = 0;
        for (ScaleTier tier : tiers) {
            if (tier == null) {
                continue;
            }
            if (tier.fromQty() <= previous) {
                return false;
            }
            previous = tier.fromQty();
        }
        return true;
    }
}
