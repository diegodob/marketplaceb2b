package com.marketplaceb2b.dto;

import java.math.BigDecimal;

/** Whether a credit order fits the customer's available credit, with the amount and a reason. */
public record CreditFit(
        boolean fits,
        BigDecimal available,
        BigDecimal total,
        CreditFitReason reason) {
}
