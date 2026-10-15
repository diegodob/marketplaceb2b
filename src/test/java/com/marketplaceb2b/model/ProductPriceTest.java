package com.marketplaceb2b.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ProductPriceTest {

    @Test
    void keepsUnitPriceScale() {
        ProductPrice price = new ProductPrice(1L, 2L, 3L, 4L, new BigDecimal("4000.0005"));

        assertThat(price.unitPrice()).isEqualTo(new BigDecimal("4000.0005"));
        assertThat(price.unitPrice().scale()).isEqualTo(4);
    }
}
