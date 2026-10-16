package com.marketplaceb2b.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class CreditLedgerTest extends AbstractDatabaseTest {

    @Test
    void ledgerExplainsTheAvailableBalance() {
        long tenant = newTenant(unique("Tenant"));
        long customer = newCustomer(tenant, unique("F"));
        long account = newCreditAccount(customer, "1000.00", "1000.00");
        long ruleset = newRuleset(tenant);
        long payment = newPaymentMethod(tenant, unique("PAY"), true);
        long order = newSalesOrder(tenant, customer, newQuote(tenant, customer, ruleset, payment, "300.00"));

        newLedgerEntry(account, order, "-300.00", "CONSUMPTION");
        run("UPDATE credit_account SET available = available - 300.00 WHERE id = ?", account);

        BigDecimal limit = jdbc.queryForObject("SELECT credit_limit FROM credit_account WHERE id = ?", BigDecimal.class, account);
        BigDecimal available = jdbc.queryForObject("SELECT available FROM credit_account WHERE id = ?", BigDecimal.class, account);
        BigDecimal movements = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM credit_ledger WHERE credit_account_id = ?", BigDecimal.class, account);

        assertThat(limit.add(movements)).isEqualByComparingTo(available);
    }
}
