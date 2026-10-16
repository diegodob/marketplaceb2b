package com.marketplaceb2b.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.marketplaceb2b.dto.Cart;
import com.marketplaceb2b.dto.CartLine;
import com.marketplaceb2b.dto.PriceBreakdown;
import com.marketplaceb2b.service.exception.MissingPriceException;
import com.marketplaceb2b.support.AbstractIntegrationTest;

class TenantIsolationTest extends AbstractIntegrationTest {

    @Autowired
    private OrderPricingService service;

    private long productId(long tenantId, String sku) {
        return jdbc.queryForObject("SELECT id FROM product WHERE tenant_id = ? AND sku = ?", Long.class, tenantId, sku);
    }

    @Test
    void pricesEachTenantWithItsOwnCurrencyRatesAndScale() {
        PriceBreakdown guatemala = service.price(new Cart(gtTenantId(), gtCustomerId(), paymentMethodId("CASH"),
                List.of(new CartLine(productId(gtTenantId(), "GAS-500"), 10))));
        PriceBreakdown colombia = service.price(new Cart(coTenantId(), coCustomerId(), paymentMethodId(coTenantId(), "CASH"),
                List.of(new CartLine(productId(coTenantId(), "GAS-400"), 10))));

        assertThat(guatemala.currency()).isEqualTo("GTQ");
        assertThat(guatemala.totalTax()).isEqualByComparingTo("4800.00");
        assertThat(guatemala.total()).isEqualByComparingTo("44800.00");
        assertThat(guatemala.total().scale()).isEqualTo(2);

        assertThat(colombia.currency()).isEqualTo("COP");
        assertThat(colombia.totalTax()).isEqualByComparingTo("4750");
        assertThat(colombia.total()).isEqualByComparingTo("29750");
        assertThat(colombia.total().scale()).isZero();
    }

    @Test
    void doesNotPriceAnotherTenantsProduct() {
        Cart colombia = new Cart(coTenantId(), coCustomerId(), paymentMethodId(coTenantId(), "CASH"),
                List.of(new CartLine(productId(gtTenantId(), "GAS-500"), 1)));

        assertThatThrownBy(() -> service.price(colombia)).isInstanceOf(MissingPriceException.class);
    }

    @Test
    void keepsProductsAndLeversScopedToTheirTenant() {
        assertThat(productId(gtTenantId(), "GAS-500")).isPositive();
        assertThat(productId(coTenantId(), "GAS-400")).isPositive();

        assertThat(countProducts(gtTenantId())).isEqualTo(3);
        assertThat(countProducts(coTenantId())).isEqualTo(2);
        assertThat(countProductsWithSku(coTenantId(), "GAS-500")).isZero();
        assertThat(countProductsWithSku(gtTenantId(), "GAS-400")).isZero();

        assertThat(count("SELECT COUNT(*) FROM scale_discount WHERE ruleset_id = ?", gtRulesetId())).isPositive();
        assertThat(count("SELECT COUNT(*) FROM combo WHERE ruleset_id = ?", gtRulesetId())).isPositive();
        assertThat(count("SELECT COUNT(*) FROM gift WHERE ruleset_id = ?", gtRulesetId())).isPositive();

        assertThat(count("SELECT COUNT(*) FROM scale_discount WHERE ruleset_id = ?", coRulesetId())).isZero();
        assertThat(count("SELECT COUNT(*) FROM combo WHERE ruleset_id = ?", coRulesetId())).isZero();
        assertThat(count("SELECT COUNT(*) FROM gift WHERE ruleset_id = ?", coRulesetId())).isZero();
    }

    private int countProducts(long tenantId) {
        return count("SELECT COUNT(*) FROM product WHERE tenant_id = ?", tenantId);
    }

    private int countProductsWithSku(long tenantId, String sku) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM product WHERE tenant_id = ? AND sku = ?", Integer.class,
                tenantId, sku);
    }

    private int count(String sql, long id) {
        return jdbc.queryForObject(sql, Integer.class, id);
    }
}
