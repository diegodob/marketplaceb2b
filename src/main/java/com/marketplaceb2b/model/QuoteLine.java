package com.marketplaceb2b.model;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record QuoteLine(
        Long id,
        @NotNull Long quoteId,
        @NotNull Long productId,
        @Positive int qty,
        @NotNull BigDecimal unitPrice,
        @NotNull BigDecimal taxableBase,
        @NotNull BigDecimal taxRate,
        @NotNull BigDecimal tax) {
}
