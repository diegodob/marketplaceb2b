package com.marketplaceb2b.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record Quote(
        Long id,
        @NotNull UUID publicId,
        @NotNull Long tenantId,
        @NotNull Long customerId,
        @NotNull Long rulesetId,
        @NotNull Long paymentMethodId,
        @NotNull BigDecimal total,
        @NotNull Instant createdAt,
        @NotNull Instant expiresAt) {
}
