package com.marketplaceb2b.model;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ScaleTier(
        @Positive int fromQty,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal percentage) {
}
