package com.marketplaceb2b.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ConstructionTest {

    @Test
    void constructingWithViolationDoesNotThrow() {
        assertThatCode(() -> new QuoteLine(1L, 2L, 3L, 0,
                new BigDecimal("1"), new BigDecimal("1"), new BigDecimal("21"), new BigDecimal("0.21")))
                .doesNotThrowAnyException();

        QuoteLine line = new QuoteLine(1L, 2L, 3L, 0,
                new BigDecimal("1"), new BigDecimal("1"), new BigDecimal("21"), new BigDecimal("0.21"));
        assertThat(line.qty()).isZero();
    }
}
