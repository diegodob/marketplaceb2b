package com.marketplaceb2b.model;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;

public record SalesOrder(
        Long id,
        @NotNull String publicNumber,
        @NotNull Long tenantId,
        @NotNull Long quoteId,
        @NotNull Long customerId,
        @NotNull Instant createdAt,
        Instant deletedAt) {
}
