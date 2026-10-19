package com.marketplaceb2b.dto;

import java.time.Instant;
import java.util.UUID;

/** The 201 response of a quote creation: its public id, its expiry and the frozen breakdown. */
public record CreatedQuote(
        UUID publicId,
        Instant expiresAt,
        PriceBreakdown breakdown) {
}
