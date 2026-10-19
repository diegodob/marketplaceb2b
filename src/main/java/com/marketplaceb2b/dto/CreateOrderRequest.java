package com.marketplaceb2b.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** The confirmation request: only the client's reference, never an amount. */
public record CreateOrderRequest(@NotNull UUID quotePublicId) {
}
