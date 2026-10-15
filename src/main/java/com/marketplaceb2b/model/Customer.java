package com.marketplaceb2b.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record Customer(
        Long id,
        @NotNull Long tenantId,
        @NotNull String name,
        @NotNull @Pattern(regexp = "[A-Za-z0-9]+") @Size(max = 30) String fiscalId,
        @NotNull Long fiscalIdTypeId) {
}
