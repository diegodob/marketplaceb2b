package com.marketplaceb2b.model;

import jakarta.validation.constraints.NotNull;

public record IdempotencyKey(
        Long id,
        @NotNull Long customerId,
        @NotNull String idemKey,
        @NotNull Long salesOrderId) {
}
