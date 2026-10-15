package com.marketplaceb2b.model;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record TaxRate(
        Long id,
        @NotNull Long rulesetId,
        @NotNull Long taxCategoryId,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal rate) {
}
