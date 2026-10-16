package com.marketplaceb2b.dto;

import java.math.BigDecimal;

public record DiscountBreakdown(
        DiscountOrigin origin,
        Long sourceId,
        String detail,
        BigDecimal amount) {
}
