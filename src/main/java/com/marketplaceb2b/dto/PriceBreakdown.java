package com.marketplaceb2b.dto;

import java.math.BigDecimal;
import java.util.List;

public record PriceBreakdown(
        Long tenantId,
        Long rulesetId,
        String currency,
        List<LineBreakdown> lines,
        List<DiscountBreakdown> discounts,
        List<GiftBreakdown> gifts,
        BigDecimal grossSubtotal,
        BigDecimal totalDiscounts,
        BigDecimal totalTaxableBase,
        BigDecimal totalTax,
        BigDecimal total) {
}
