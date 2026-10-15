package com.marketplaceb2b.model;

import jakarta.validation.constraints.NotNull;

public record FiscalIdType(
        Long id,
        @NotNull Long countryId,
        @NotNull String code,
        @NotNull String name) {
}
