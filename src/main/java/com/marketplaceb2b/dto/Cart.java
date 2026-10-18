package com.marketplaceb2b.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record Cart(
        @NotNull(message = "es obligatorio") Long tenantId,
        Long customerId,
        Long paymentMethodId,
        @Valid @NotEmpty(message = "debe tener al menos una línea") List<@NotNull(message = "no puede ser nula") CartLine> lines) {
}
