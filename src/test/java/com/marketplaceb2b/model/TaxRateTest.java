package com.marketplaceb2b.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class TaxRateTest {

    @Test
    void keepsRateScale() {
        TaxRate rate = new TaxRate(1L, 2L, 3L, new BigDecimal("10.5000"));

        assertThat(rate.rate()).isEqualTo(new BigDecimal("10.5000"));
        assertThat(rate.rate().scale()).isEqualTo(4);
    }
}
