package com.marketplaceb2b.model;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GiftCondition(
        @NotNull GiftConditionType type,
        Long productId,
        String family,
        @Positive int minQty) {

    @AssertTrue(message = "condition must be exactly one of productId (PRODUCT_QTY) or family (FAMILY_QTY)")
    public boolean isConsistent() {
        if (type == GiftConditionType.PRODUCT_QTY) {
            return productId != null && family == null;
        }
        if (type == GiftConditionType.FAMILY_QTY) {
            return family != null && productId == null;
        }
        return false;
    }
}
