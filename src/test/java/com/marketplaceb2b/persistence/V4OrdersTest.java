package com.marketplaceb2b.persistence;

import org.junit.jupiter.api.Test;

class V4OrdersTest extends AbstractDatabaseTest {

    @Test
    void rejectsSecondOrderForQuote() {
        long tenant = newTenant(unique("Tenant"));
        long customer = newCustomer(tenant, unique("F"));
        long ruleset = newRuleset(tenant);
        long payment = newPaymentMethod(tenant, unique("PAY"), false);
        long quote = newQuote(tenant, customer, ruleset, payment, "100.00");

        newSalesOrder(tenant, customer, quote);
        assertRejected("INSERT INTO sales_order (public_number, tenant_id, quote_id, customer_id, created_at) "
                + "VALUES (?, ?, ?, ?, ?)", java.util.UUID.randomUUID().toString(), tenant, quote, customer,
                ts("2026-01-01 00:00:00"));
    }

    @Test
    void rejectsDuplicatePublicNumber() {
        long tenant = newTenant(unique("Tenant"));
        long customer = newCustomer(tenant, unique("F"));
        long ruleset = newRuleset(tenant);
        long payment = newPaymentMethod(tenant, unique("PAY"), false);
        long quote1 = newQuote(tenant, customer, ruleset, payment, "100.00");
        long quote2 = newQuote(tenant, customer, ruleset, payment, "100.00");
        String publicNumber = "ORDER-1";

        run("INSERT INTO sales_order (public_number, tenant_id, quote_id, customer_id, created_at) VALUES (?, ?, ?, ?, ?)",
                publicNumber, tenant, quote1, customer, ts("2026-01-01 00:00:00"));
        assertRejected("INSERT INTO sales_order (public_number, tenant_id, quote_id, customer_id, created_at) VALUES (?, ?, ?, ?, ?)",
                publicNumber, tenant, quote2, customer, ts("2026-01-01 00:00:00"));
    }

    @Test
    void rejectsDuplicateIdempotencyKey() {
        long tenant = newTenant(unique("Tenant"));
        long customer = newCustomer(tenant, unique("F"));
        long ruleset = newRuleset(tenant);
        long payment = newPaymentMethod(tenant, unique("PAY"), false);
        long order = newSalesOrder(tenant, customer, newQuote(tenant, customer, ruleset, payment, "100.00"));

        newIdempotencyKey(customer, "IDEM-1", order);
        assertRejected("INSERT INTO idempotency_key (customer_id, idem_key, sales_order_id) VALUES (?, 'IDEM-1', ?)",
                customer, order);
    }

    @Test
    void rejectsSecondConsumptionForOrder() {
        long tenant = newTenant(unique("Tenant"));
        long customer = newCustomer(tenant, unique("F"));
        long account = newCreditAccount(customer, "1000", "1000");
        long ruleset = newRuleset(tenant);
        long payment = newPaymentMethod(tenant, unique("PAY"), true);
        long order = newSalesOrder(tenant, customer, newQuote(tenant, customer, ruleset, payment, "100.00"));

        newLedgerEntry(account, order, "-100.00", "CONSUMPTION");
        assertRejected("INSERT INTO credit_ledger (credit_account_id, sales_order_id, amount, entry_type, created_at) "
                + "VALUES (?, ?, -50.00, 'CONSUMPTION', ?)", account, order, ts("2026-01-01 00:00:00"));
    }

    @Test
    void rejectsDuplicateOutboxEvent() {
        long tenant = newTenant(unique("Tenant"));
        long customer = newCustomer(tenant, unique("F"));
        long ruleset = newRuleset(tenant);
        long payment = newPaymentMethod(tenant, unique("PAY"), false);
        long order = newSalesOrder(tenant, customer, newQuote(tenant, customer, ruleset, payment, "100.00"));

        newOutbox(order, "PUSH", "ORDER_CONFIRMED", "PENDING");
        assertRejected("INSERT INTO outbox (sales_order_id, destination, event_type, payload, status, created_at) "
                + "VALUES (?, 'PUSH', 'ORDER_CONFIRMED', '{}', 'PENDING', ?)", order, ts("2026-01-01 00:00:00"));
    }

    @Test
    void rejectsInvalidLedgerEntryType() {
        long tenant = newTenant(unique("Tenant"));
        long customer = newCustomer(tenant, unique("F"));
        long account = newCreditAccount(customer, "1000", "1000");
        long ruleset = newRuleset(tenant);
        long payment = newPaymentMethod(tenant, unique("PAY"), true);
        long order = newSalesOrder(tenant, customer, newQuote(tenant, customer, ruleset, payment, "100.00"));

        assertRejected("INSERT INTO credit_ledger (credit_account_id, sales_order_id, amount, entry_type, created_at) "
                + "VALUES (?, ?, -1.00, 'NOPE', ?)", account, order, ts("2026-01-01 00:00:00"));
    }

    @Test
    void rejectsInvalidOutboxEnums() {
        long tenant = newTenant(unique("Tenant"));
        long customer = newCustomer(tenant, unique("F"));
        long ruleset = newRuleset(tenant);
        long payment = newPaymentMethod(tenant, unique("PAY"), false);
        long order = newSalesOrder(tenant, customer, newQuote(tenant, customer, ruleset, payment, "100.00"));

        assertRejected("INSERT INTO outbox (sales_order_id, destination, event_type, payload, status, created_at) "
                + "VALUES (?, 'SMS', 'ORDER_CONFIRMED', '{}', 'PENDING', ?)", order, ts("2026-01-01 00:00:00"));
        assertRejected("INSERT INTO outbox (sales_order_id, destination, event_type, payload, status, created_at) "
                + "VALUES (?, 'PUSH', 'NOPE', '{}', 'PENDING', ?)", order, ts("2026-01-01 00:00:00"));
        assertRejected("INSERT INTO outbox (sales_order_id, destination, event_type, payload, status, created_at) "
                + "VALUES (?, 'PUSH', 'ORDER_CONFIRMED', '{}', 'NOPE', ?)", order, ts("2026-01-01 00:00:00"));
    }
}
