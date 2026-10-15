package com.marketplaceb2b.model;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;

public record Ruleset(
        Long id,
        @NotNull Long tenantId,
        @NotNull String version,
        @NotNull String currency,
        @NotNull Integer currencyScale,
        @NotNull RoundingMode roundingMode,
        @NotNull Instant effectiveFrom) {
}
