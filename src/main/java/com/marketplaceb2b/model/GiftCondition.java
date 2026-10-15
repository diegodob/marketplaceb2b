package com.marketplaceb2b.model;

import java.math.BigDecimal;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record GiftCondition(
        Long productId,
        Long familyId,
        @Positive Integer minQty,
        @PositiveOrZero BigDecimal minAmount) {

    @AssertTrue(message = "a condition needs a quantity and/or an amount threshold; a quantity needs exactly one target")
    public boolean isWellFormed() {
        if (productId != null && familyId != null) {
            return false;
        }
        boolean hasQuantity = minQty != null;
        boolean hasAmount = minAmount != null;
        if (!hasQuantity && !hasAmount) {
            return false;
        }
        return !hasQuantity || productId != null || familyId != null;
    }
}
