package com.marketplaceb2b.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record Gift(
        Long id,
        @NotNull Long rulesetId,
        @NotNull @Valid GiftCondition condition,
        @NotNull Long giftProductId,
        @Positive int giftQty,
        boolean taxable,
        int priority) {
}
