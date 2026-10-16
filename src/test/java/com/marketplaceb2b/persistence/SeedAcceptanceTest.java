package com.marketplaceb2b.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class SeedAcceptanceTest extends AbstractDatabaseTest {

    private long id(String sql) {
        return jdbc.queryForObject(sql, Long.class);
    }

    @Test
    void confirmedOrderMatchesTheQuotedTotal() {
        long tenant = id("SELECT id FROM tenant WHERE name = 'Bebidas AR'");
        long ruleset = id("SELECT id FROM ruleset WHERE version = 'AR-2026-10-01.1'");
        long customer = id("SELECT id FROM customer WHERE name = 'Cliente 12' AND tenant_id = " + tenant);
        long credit = id("SELECT id FROM payment_method WHERE code = 'CREDIT' AND tenant_id = " + tenant);
        long gas = id("SELECT id FROM product WHERE sku = 'GAS-500' AND tenant_id = " + tenant);
        long yer = id("SELECT id FROM product WHERE sku = 'YER-1KG' AND tenant_id = " + tenant);
        long agu = id("SELECT id FROM product WHERE sku = 'AGU-500' AND tenant_id = " + tenant);

        long quote = newQuote(tenant, customer, ruleset, credit, "93070.00");
        newQuoteLine(quote, gas, 10, "4000.0000", "40000.00", "21.0000", "8400.00");
        newQuoteLine(quote, yer, 4, "6000.0000", "24000.00", "10.5000", "2520.00");
        newQuoteLine(quote, agu, 6, "2500.0000", "15000.00", "21.0000", "3150.00");

        BigDecimal sum = jdbc.queryForObject(
                "SELECT SUM(taxable_base + tax) FROM quote_line WHERE quote_id = ?", BigDecimal.class, quote);
        BigDecimal total = jdbc.queryForObject("SELECT total FROM quote WHERE id = ?", BigDecimal.class, quote);

        assertThat(sum).isEqualByComparingTo("93070.00");
        assertThat(total).isEqualByComparingTo(sum);

        List<BigDecimal> storedAmounts = jdbc.query(
                "SELECT taxable_base FROM quote_line WHERE quote_id = ? UNION ALL "
                        + "SELECT tax FROM quote_line WHERE quote_id = ?",
                (rs, rowNum) -> rs.getBigDecimal(1), quote, quote);
        assertThat(storedAmounts).allSatisfy(amount ->
                assertThat(Math.max(amount.stripTrailingZeros().scale(), 0)).isLessThanOrEqualTo(2));
        assertThat(Math.max(total.stripTrailingZeros().scale(), 0)).isLessThanOrEqualTo(2);
    }

    @Test
    void confirmedOrderEnqueuesTwoPendingEffects() {
        long tenant = id("SELECT id FROM tenant WHERE name = 'Bebidas AR'");
        long ruleset = id("SELECT id FROM ruleset WHERE version = 'AR-2026-10-01.1'");
        long customer = id("SELECT id FROM customer WHERE name = 'Cliente 12' AND tenant_id = " + tenant);
        long credit = id("SELECT id FROM payment_method WHERE code = 'CREDIT' AND tenant_id = " + tenant);

        long order = newSalesOrder(tenant, customer, newQuote(tenant, customer, ruleset, credit, "93070.00"));
        newOutbox(order, "PUSH", "ORDER_CONFIRMED", "PENDING");
        newOutbox(order, "ERP", "ORDER_CONFIRMED", "PENDING");

        Integer pending = jdbc.queryForObject(
                "SELECT COUNT(*) FROM outbox WHERE sales_order_id = ? AND status = 'PENDING' AND event_type = 'ORDER_CONFIRMED'",
                Integer.class, order);
        assertThat(pending).isEqualTo(2);
    }

    @Test
    void replayingTheConfirmationKeyIsRejected() {
        long tenant = id("SELECT id FROM tenant WHERE name = 'Bebidas AR'");
        long ruleset = id("SELECT id FROM ruleset WHERE version = 'AR-2026-10-01.1'");
        long customer = id("SELECT id FROM customer WHERE name = 'Cliente 12' AND tenant_id = " + tenant);
        long credit = id("SELECT id FROM payment_method WHERE code = 'CREDIT' AND tenant_id = " + tenant);

        long order = newSalesOrder(tenant, customer, newQuote(tenant, customer, ruleset, credit, "93070.00"));
        newIdempotencyKey(customer, "IDEM-SEED", order);

        assertRejected("INSERT INTO idempotency_key (customer_id, idem_key, sales_order_id) VALUES (?, 'IDEM-SEED', ?)",
                customer, order);
    }

    @Test
    void salesOrderHasNoTotalColumn() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'sales_order' AND column_name = 'total'",
                Integer.class);
        assertThat(count).isZero();
    }

    @Test
    void frozenQuoteValuesSurviveALaterPriceChange() {
        long tenant = id("SELECT id FROM tenant WHERE name = 'Bebidas AR'");
        long ruleset = id("SELECT id FROM ruleset WHERE version = 'AR-2026-10-01.1'");
        long customer = id("SELECT id FROM customer WHERE name = 'Cliente 12' AND tenant_id = " + tenant);
        long payment = id("SELECT id FROM payment_method WHERE code = 'CASH' AND tenant_id = " + tenant);
        long product = id("SELECT id FROM product WHERE sku = 'GAS-500' AND tenant_id = " + tenant);

        long quote = newQuote(tenant, customer, ruleset, payment, "93070.00");
        newQuoteLine(quote, product, 10, "4000.0000", "40000.00", "21.0000", "8400.00");

        run("UPDATE product_price SET unit_price = 9999.0000 WHERE ruleset_id = ? AND product_id = ?",
                ruleset, product);

        assertThat(jdbc.queryForObject("SELECT unit_price FROM quote_line WHERE quote_id = ?", BigDecimal.class, quote))
                .isEqualByComparingTo("4000.0000");
        assertThat(jdbc.queryForObject("SELECT total FROM quote WHERE id = ?", BigDecimal.class, quote))
                .isEqualByComparingTo("93070.00");
    }
}
