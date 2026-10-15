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
        String family,
        @NotNull @Size(min = 1) @Valid List<ScaleTier> tiers) {

    @AssertTrue(message = "target must be exactly one of productId (SKU) or family (FAMILY)")
    public boolean isTargetConsistent() {
        if (targetType == DiscountTargetType.SKU) {
            return productId != null && family == null;
        }
        if (targetType == DiscountTargetType.FAMILY) {
            return family != null && productId == null;
        }
        return false;
    }
}
