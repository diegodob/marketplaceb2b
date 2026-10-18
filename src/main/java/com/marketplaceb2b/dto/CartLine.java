package com.marketplaceb2b.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartLine(
        @NotNull(message = "es obligatorio") Long productId,
        @Positive(message = "debe ser mayor que cero") int qty) {
}
