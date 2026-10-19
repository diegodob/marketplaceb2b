package com.marketplaceb2b.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.marketplaceb2b.model.QuoteLine;

/** The queryable order: the header plus the frozen lines. */
public record OrderView(
        String number,
        UUID quotePublicId,
        BigDecimal total,
        Instant createdAt,
        List<QuoteLine> lines) {
}
