package com.marketplaceb2b.dto;

import java.util.List;

public record Cart(
        Long tenantId,
        Long customerId,
        Long paymentMethodId,
        List<CartLine> lines) {
}
