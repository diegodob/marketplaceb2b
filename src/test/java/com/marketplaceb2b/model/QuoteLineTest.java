package com.marketplaceb2b.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class QuoteLineTest {

    @Test
    void carriesMonetaryValuesUnchanged() {
        QuoteLine line = new QuoteLine(
                1L, 2L, 3L, 4,
                new BigDecimal("4000.0000"),
                new BigDecimal("40000.004"),
                new BigDecimal("21.0000"),
                new BigDecimal("8400.0008"));

        assertThat(line.unitPrice()).isEqualTo(new BigDecimal("4000.0000"));
        assertThat(line.taxableBase()).isEqualTo(new BigDecimal("40000.004"));
        assertThat(line.taxRate()).isEqualTo(new BigDecimal("21.0000"));
        assertThat(line.tax()).isEqualTo(new BigDecimal("8400.0008"));
    }
}
