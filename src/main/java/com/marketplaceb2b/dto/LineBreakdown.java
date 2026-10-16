package com.marketplaceb2b.dto;

import java.math.BigDecimal;

public record LineBreakdown(
        Long productId,
        int qty,
        BigDecimal unitPrice,
        BigDecimal gross,
        BigDecimal discount,
        BigDecimal taxableBase,
        BigDecimal taxRate,
        BigDecimal tax) {
}
