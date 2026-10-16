package com.marketplaceb2b.service.dto;

import java.math.BigDecimal;

public record LineBreakdown(
        Long productId,
        int qty,
        BigDecimal unitPrice,
        BigDecimal gross,
        BigDecimal taxableBase,
        BigDecimal taxRate,
        BigDecimal tax) {
}
