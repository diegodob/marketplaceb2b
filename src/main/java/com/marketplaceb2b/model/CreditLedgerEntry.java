package com.marketplaceb2b.model;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.NotNull;

public record CreditLedgerEntry(
        Long id,
        @NotNull Long creditAccountId,
        @NotNull Long salesOrderId,
        @NotNull BigDecimal amount,
        @NotNull LedgerEntryType entryType,
        @NotNull Instant createdAt) {
}
