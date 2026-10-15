package com.marketplaceb2b.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class ModelArchitectureTest {

    private static final List<Class<?>> MODEL_TYPES = List.of(
            Country.class, Tenant.class, TaxCategory.class, Ruleset.class, TaxRate.class,
            FiscalIdType.class, Product.class, ProductPrice.class, PaymentMethod.class, Customer.class,
            CreditAccount.class, Quote.class, QuoteLine.class, SalesOrder.class,
            IdempotencyKey.class, CreditLedgerEntry.class, OutboxEvent.class);

    @Test
    void allTypesAreRecords() {
        assertThat(MODEL_TYPES).allMatch(Class::isRecord);
    }

    @Test
    void noTypeExposesSetters() {
        for (Class<?> type : MODEL_TYPES) {
            for (Method method : type.getMethods()) {
                assertThat(method.getName()).doesNotStartWith("set");
            }
        }
    }

    @Test
    void noComponentUsesFloatingPoint() {
        for (Class<?> type : MODEL_TYPES) {
            for (RecordComponent component : type.getRecordComponents()) {
                assertThat(component.getType())
                        .isNotIn(float.class, double.class, Float.class, Double.class);
            }
        }
    }

    @Test
    void monetaryComponentsAreBigDecimal() {
        assertBigDecimal(Quote.class, "total");
        assertBigDecimal(QuoteLine.class, "unitPrice", "taxableBase", "taxRate", "tax");
        assertBigDecimal(ProductPrice.class, "unitPrice");
        assertBigDecimal(TaxRate.class, "rate");
        assertBigDecimal(CreditAccount.class, "creditLimit", "available");
        assertBigDecimal(CreditLedgerEntry.class, "amount");
    }

    private static void assertBigDecimal(Class<?> type, String... names) {
        for (String name : names) {
            try {
                assertThat(type.getMethod(name).getReturnType()).isEqualTo(BigDecimal.class);
            } catch (NoSuchMethodException e) {
                throw new AssertionError(e);
            }
        }
    }
}
