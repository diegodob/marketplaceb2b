package com.marketplaceb2b.dto;

import java.math.BigDecimal;

public record DiscountBreakdown(
        DiscountOrigin origin,
        String detail,
        BigDecimal amount) {
}
