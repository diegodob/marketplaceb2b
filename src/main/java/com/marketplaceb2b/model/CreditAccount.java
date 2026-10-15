package com.marketplaceb2b.model;

import java.math.BigDecimal;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record CreditAccount(
        Long id,
        @NotNull Long customerId,
        @NotNull String currency,
        @NotNull BigDecimal creditLimit,
        @NotNull BigDecimal available) {

    @AssertTrue(message = "available must be within 0..creditLimit")
    public boolean isAvailableWithinLimit() {
        if (available == null || creditLimit == null) {
            return true;
        }
        return available.compareTo(BigDecimal.ZERO) >= 0
                && available.compareTo(creditLimit) <= 0;
    }
}
