package com.marketplaceb2b.model;

import jakarta.validation.constraints.NotNull;

public record PaymentMethod(
        Long id,
        @NotNull Long tenantId,
        @NotNull String code,
        @NotNull String name,
        boolean usesCredit) {
}
