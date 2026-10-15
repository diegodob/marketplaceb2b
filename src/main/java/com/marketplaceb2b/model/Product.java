package com.marketplaceb2b.model;

import jakarta.validation.constraints.NotNull;

public record Product(
        Long id,
        @NotNull Long tenantId,
        @NotNull String sku,
        @NotNull String name,
        String family) {
}
