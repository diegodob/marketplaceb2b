package com.marketplaceb2b.model;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;

public record OutboxEvent(
        Long id,
        @NotNull Long salesOrderId,
        @NotNull OutboxDestination destination,
        @NotNull OutboxEventType eventType,
        @NotNull String payload,
        @NotNull OutboxStatus status,
        int attempts,
        Instant nextAttemptAt,
        String lastError,
        @NotNull Instant createdAt,
        Instant sentAt) {
}
