package com.marketplaceb2b.service.model;

import java.util.List;

public record Cart(
        Long tenantId,
        Long customerId,
        Long paymentMethodId,
        List<CartLine> lines) {
}
