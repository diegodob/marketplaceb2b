package com.marketplaceb2b.model;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record Combo(
        Long id,
        @NotNull Long rulesetId,
        @NotNull String name,
        @NotNull @Size(min = 1) @Valid List<@NotNull ComboItem> items,
        @NotNull @PositiveOrZero BigDecimal comboPrice,
        int priority) {
}
