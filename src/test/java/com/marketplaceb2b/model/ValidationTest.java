package com.marketplaceb2b.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void detectsNonPositiveQuantity() {
        QuoteLine line = new QuoteLine(1L, 2L, 3L, 0,
                new BigDecimal("1"), new BigDecimal("1"), new BigDecimal("21"), new BigDecimal("0.21"));
        assertThat(validator.validate(line)).isNotEmpty();
    }

    @Test
    void detectsOutOfRangeRate() {
        TaxRate rate = new TaxRate(1L, 2L, 3L, new BigDecimal("150"));
        assertThat(validator.validate(rate)).isNotEmpty();
    }

    @Test
    void detectsCreditAvailableOutOfBounds() {
        CreditAccount account = new CreditAccount(1L, 2L, "ARS",
                new BigDecimal("100"), new BigDecimal("150"));
        assertThat(validator.validate(account)).isNotEmpty();
    }

    @Test
    void detectsMissingRequiredComponent() {
        Quote quote = new Quote(1L, null, 1L, 1L, 1L, 1L,
                new BigDecimal("1"), Instant.now(), Instant.now());
        assertThat(validator.validate(quote)).isNotEmpty();
    }

    @Test
    void acceptsValidInstances() {
        CreditAccount account = new CreditAccount(1L, 2L, "ARS",
                new BigDecimal("500000.00"), new BigDecimal("300000.00"));
        assertThat(validator.validate(account)).isEmpty();
    }

    @Test
    void detectsNonAlphanumericFiscalId() {
        Customer customer = new Customer(1L, 2L, "Tienda", "CUIT-20", 1L);
        assertThat(validator.validate(customer)).isNotEmpty();
    }

    @Test
    void acceptsAlphanumericFiscalId() {
        Customer customer = new Customer(1L, 2L, "Tienda", "20304050607", 1L);
        assertThat(validator.validate(customer)).isEmpty();
    }
}
