package com.marketplaceb2b.dto;

import java.math.BigDecimal;

public record GiftBreakdown(
        Long productId,
        int qty,
        boolean taxable,
        BigDecimal tax) {
}
