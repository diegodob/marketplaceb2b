package com.marketplaceb2b.persistence;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class V3QuotesTest extends AbstractDatabaseTest {

    @Test
    void rejectsDuplicateQuotePublicId() {
        long tenant = newTenant(unique("Tenant"));
        long customer = newCustomer(tenant, unique("F"));
        long ruleset = newRuleset(tenant);
        long payment = newPaymentMethod(tenant, unique("PAY"), true);
        UUID publicId = UUID.randomUUID();

        newQuoteWithPublicId(publicId, tenant, customer, ruleset, payment, "100.00");
        assertRejected("INSERT INTO quote (public_id, tenant_id, customer_id, ruleset_id, payment_method_id, total, created_at, expires_at) "
                + "VALUES (?, ?, ?, ?, ?, 100.00, ?, ?)",
                publicId, tenant, customer, ruleset, payment, ts("2026-01-01 00:00:00"), ts("2026-01-02 00:00:00"));
    }

    @Test
    void rejectsDuplicateLinePerProduct() {
        long tenant = newTenant(unique("Tenant"));
        long customer = newCustomer(tenant, unique("F"));
        long ruleset = newRuleset(tenant);
        long payment = newPaymentMethod(tenant, unique("PAY"), false);
        long product = newProduct(tenant, unique("SKU"));
        long quote = newQuote(tenant, customer, ruleset, payment, "100.00");

        newQuoteLine(quote, product, 2, "10.00", "20.00", "21.0000", "4.20");
        assertRejected("INSERT INTO quote_line (quote_id, product_id, qty, unit_price, taxable_base, tax_rate, tax) "
                + "VALUES (?, ?, 1, 10.00, 10.00, 21.0000, 2.10)", quote, product);
    }

    @Test
    void rejectsNonPositiveQuantity() {
        long tenant = newTenant(unique("Tenant"));
        long customer = newCustomer(tenant, unique("F"));
        long ruleset = newRuleset(tenant);
        long payment = newPaymentMethod(tenant, unique("PAY"), false);
        long product = newProduct(tenant, unique("SKU"));
        long quote = newQuote(tenant, customer, ruleset, payment, "0.00");

        assertRejected("INSERT INTO quote_line (quote_id, product_id, qty, unit_price, taxable_base, tax_rate, tax) "
                + "VALUES (?, ?, 0, 10.00, 0.00, 21.0000, 0.00)", quote, product);
    }
}
