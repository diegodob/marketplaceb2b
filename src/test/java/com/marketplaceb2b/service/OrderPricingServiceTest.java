package com.marketplaceb2b.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.marketplaceb2b.service.exception.MissingPriceException;
import com.marketplaceb2b.service.exception.MissingTaxRateException;
import com.marketplaceb2b.service.exception.NoCurrentRulesetException;
import com.marketplaceb2b.service.model.Cart;
import com.marketplaceb2b.service.model.CartLine;
import com.marketplaceb2b.service.model.LineBreakdown;
import com.marketplaceb2b.service.model.PriceBreakdown;
import com.marketplaceb2b.support.AbstractIntegrationTest;

class OrderPricingServiceTest extends AbstractIntegrationTest {

    @Autowired
    private OrderPricingService service;

    private long productId(String sku) {
        return jdbc.queryForObject("SELECT id FROM product WHERE sku = ?", Long.class, sku);
    }

    @Test
    void pricesASeededCart() {
        Cart cart = new Cart(arTenantId(), arCustomerId(), paymentMethodId("CASH"), List.of(
                new CartLine(productId("GAS-500"), 10),
                new CartLine(productId("YER-1KG"), 4),
                new CartLine(productId("AGU-500"), 6)));

        PriceBreakdown breakdown = service.price(cart);

        assertThat(breakdown.currency()).isEqualTo("ARS");
        assertThat(breakdown.lines()).hasSize(3);
        assertThat(breakdown.grossSubtotal()).isEqualByComparingTo("79000.00");
        assertThat(breakdown.totalTax()).isEqualByComparingTo("14070.00");
        assertThat(breakdown.totalTaxableBase()).isEqualByComparingTo("79000.00");
        assertThat(breakdown.total()).isEqualByComparingTo("93070.00");
        assertThat(breakdown.discounts()).isEmpty();
        assertThat(breakdown.gifts()).isEmpty();
        assertThat(breakdown.totalDiscounts()).isEqualByComparingTo("0.00");
    }

    @Test
    void failsWithoutRuleset() {
        long tenant = newTenant(unique("Tenant"));
        Cart cart = new Cart(tenant, null, null, List.of(new CartLine(1L, 1)));

        assertThatThrownBy(() -> service.price(cart)).isInstanceOf(NoCurrentRulesetException.class);
    }

    @Test
    void failsWhenAProductHasNoPrice() {
        long product = insert("INSERT INTO product (tenant_id, sku, name) VALUES (?, ?, 'No price')",
                arTenantId(), unique("SKU"));
        Cart cart = new Cart(arTenantId(), null, null, List.of(new CartLine(product, 1)));

        assertThatThrownBy(() -> service.price(cart)).isInstanceOf(MissingPriceException.class);
    }

    @Test
    void failsWhenACategoryHasNoRate() {
        long category = insert("INSERT INTO tax_category (code, name) VALUES (?, 'No rate')", unique("CAT"));
        long product = insert("INSERT INTO product (tenant_id, sku, name) VALUES (?, ?, 'No rate')",
                arTenantId(), unique("SKU"));
        insert("INSERT INTO product_price (ruleset_id, product_id, tax_category_id, unit_price) VALUES (?, ?, ?, 10.0000)",
                arRulesetId(), product, category);
        Cart cart = new Cart(arTenantId(), null, null, List.of(new CartLine(product, 1)));

        assertThatThrownBy(() -> service.price(cart)).isInstanceOf(MissingTaxRateException.class);
    }

    @Test
    void roundsEachLineToTheCurrencyScale() {
        long category = insert("INSERT INTO tax_category (code, name) VALUES (?, 'Round')", unique("CAT"));
        insert("INSERT INTO tax_rate (ruleset_id, tax_category_id, rate) VALUES (?, ?, 21.0000)",
                arRulesetId(), category);
        long product = insert("INSERT INTO product (tenant_id, sku, name) VALUES (?, ?, 'Round')",
                arTenantId(), unique("SKU"));
        insert("INSERT INTO product_price (ruleset_id, product_id, tax_category_id, unit_price) VALUES (?, ?, ?, 3.3350)",
                arRulesetId(), product, category);
        Cart cart = new Cart(arTenantId(), null, null, List.of(new CartLine(product, 3)));

        PriceBreakdown breakdown = service.price(cart);

        LineBreakdown line = breakdown.lines().get(0);
        assertThat(line.taxableBase()).isEqualByComparingTo("10.01");
        assertThat(line.tax()).isEqualByComparingTo("2.10");
        assertThat(breakdown.total()).isEqualByComparingTo("12.11");
    }
}
