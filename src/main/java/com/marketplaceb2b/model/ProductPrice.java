package com.marketplaceb2b.model;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ProductPrice(
        Long id,
        @NotNull Long rulesetId,
        @NotNull Long productId,
        @NotNull Long taxCategoryId,
        @NotNull @PositiveOrZero BigDecimal unitPrice) {
}
