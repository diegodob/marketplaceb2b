package com.marketplaceb2b.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import org.junit.jupiter.api.Test;

class AnnotationPresenceTest {

    private static Method accessor(Class<?> type, String name) {
        try {
            return type.getMethod(name);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void requiredComponentsAreNotNull() {
        assertThat(accessor(Quote.class, "publicId").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(accessor(QuoteLine.class, "quoteId").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(accessor(ProductPrice.class, "unitPrice").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(accessor(TaxRate.class, "rate").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(accessor(Country.class, "code").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(accessor(Tenant.class, "countryId").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(accessor(Customer.class, "fiscalId").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(accessor(Customer.class, "fiscalIdTypeId").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(accessor(FiscalIdType.class, "countryId").isAnnotationPresent(NotNull.class)).isTrue();
    }

    @Test
    void fiscalIdIsAlphanumericAndBounded() {
        Method fiscalId = accessor(Customer.class, "fiscalId");
        assertThat(fiscalId.isAnnotationPresent(Pattern.class)).isTrue();
        assertThat(fiscalId.getAnnotation(Pattern.class).regexp()).isEqualTo("[A-Za-z0-9]+");
        assertThat(fiscalId.isAnnotationPresent(Size.class)).isTrue();
        assertThat(fiscalId.getAnnotation(Size.class).max()).isEqualTo(30);
    }

    @Test
    void quoteLineQtyIsPositive() {
        assertThat(accessor(QuoteLine.class, "qty").isAnnotationPresent(Positive.class)).isTrue();
    }

    @Test
    void productPriceUnitPriceIsPositiveOrZero() {
        assertThat(accessor(ProductPrice.class, "unitPrice").isAnnotationPresent(PositiveOrZero.class)).isTrue();
    }

    @Test
    void taxRateIsBounded() {
        Method rate = accessor(TaxRate.class, "rate");
        assertThat(rate.isAnnotationPresent(DecimalMin.class)).isTrue();
        assertThat(rate.isAnnotationPresent(DecimalMax.class)).isTrue();
        assertThat(rate.getAnnotation(DecimalMin.class).value()).isEqualTo("0");
        assertThat(rate.getAnnotation(DecimalMax.class).value()).isEqualTo("100");
    }

    @Test
    void creditAccountDeclaresClassLevelRule() {
        assertThat(accessor(CreditAccount.class, "isAvailableWithinLimit")
                .isAnnotationPresent(AssertTrue.class)).isTrue();
    }
}
