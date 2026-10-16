package com.marketplaceb2b.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ComboItem(
        @NotNull Long productId,
        @Positive int minQty) {
}
