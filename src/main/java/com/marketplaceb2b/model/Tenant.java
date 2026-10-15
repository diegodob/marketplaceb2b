package com.marketplaceb2b.model;

import jakarta.validation.constraints.NotNull;

public record Tenant(
        Long id,
        @NotNull String name,
        @NotNull Long countryId) {
}
