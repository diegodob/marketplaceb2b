package com.marketplaceb2b.service;

import java.math.BigDecimal;
import java.util.Objects;

import com.marketplaceb2b.model.RoundingMode;

public record Money(BigDecimal amount, int scale, RoundingMode roundingMode) {

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(roundingMode, "roundingMode");
    }

    public static Money of(BigDecimal amount, int scale, RoundingMode roundingMode) {
        return new Money(amount.setScale(scale, toJava(roundingMode)), scale, roundingMode);
    }

    public static Money zero(int scale, RoundingMode roundingMode) {
        return of(BigDecimal.ZERO, scale, roundingMode);
    }

    public Money times(int qty) {
        return of(amount.multiply(BigDecimal.valueOf(qty)), scale, roundingMode);
    }

    public Money times(BigDecimal factor) {
        return of(amount.multiply(factor), scale, roundingMode);
    }

    public Money percentage(BigDecimal rate) {
        return of(amount.multiply(rate).movePointLeft(2), scale, roundingMode);
    }

    public Money plus(Money other) {
        if (scale != other.scale || roundingMode != other.roundingMode) {
            throw new IllegalArgumentException("Cannot combine amounts with different rounding policies");
        }
        return of(amount.add(other.amount), scale, roundingMode);
    }

    static java.math.RoundingMode toJava(RoundingMode mode) {
        return switch (mode) {
            case HALF_UP -> java.math.RoundingMode.HALF_UP;
            case HALF_EVEN -> java.math.RoundingMode.HALF_EVEN;
        };
    }
}
