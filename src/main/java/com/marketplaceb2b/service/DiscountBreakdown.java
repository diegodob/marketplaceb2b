package com.marketplaceb2b.service;

import java.math.BigDecimal;

public record DiscountBreakdown(
        DiscountOrigin origin,
        String detail,
        BigDecimal amount) {
}
