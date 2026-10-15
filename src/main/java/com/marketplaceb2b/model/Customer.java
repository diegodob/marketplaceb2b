package com.marketplaceb2b.model;

import jakarta.validation.constraints.NotNull;

public record Customer(
        Long id,
        @NotNull Long tenantId,
        @NotNull String name,
        @NotNull String fiscalId) {
}
