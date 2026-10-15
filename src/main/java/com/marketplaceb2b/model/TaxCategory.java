package com.marketplaceb2b.model;

import jakarta.validation.constraints.NotNull;

public record TaxCategory(
        Long id,
        @NotNull String code,
        @NotNull String name) {
}
