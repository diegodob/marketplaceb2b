package com.marketplaceb2b.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
abstract class AbstractDatabaseTest {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    @Autowired
    protected JdbcTemplate jdbc;

    protected static String unique(String prefix) {
        return prefix + COUNTER.incrementAndGet();
    }

    protected static Timestamp ts(String value) {
        return Timestamp.valueOf(value);
    }

    protected long insert(String sql, Object... args) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) {
                statement.setObject(i + 1, args[i]);
            }
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? 0L : key.longValue();
    }

    protected void run(String sql, Object... args) {
        jdbc.update(sql, args);
    }

    protected void assertRejected(String sql, Object... args) {
        assertThatThrownBy(() -> jdbc.update(sql, args)).isInstanceOf(DataAccessException.class);
    }

    protected long gtCountryId() {
        return jdbc.queryForObject("SELECT id FROM country WHERE code = 'GT'", Long.class);
    }

    protected long newTenant(String name) {
        return insert("INSERT INTO tenant (name, country_id) VALUES (?, ?)", name, gtCountryId());
    }

    protected long newTaxCategory(String code) {
        return insert("INSERT INTO tax_category (code, name) VALUES (?, ?)", code, code);
    }

    protected long newRuleset(long tenantId, String version, Timestamp effectiveFrom) {
        return insert("INSERT INTO ruleset (tenant_id, version, currency, currency_scale, rounding_mode, effective_from) "
                + "VALUES (?, ?, 'GTQ', 2, 'HALF_UP', ?)", tenantId, version, effectiveFrom);
    }

    protected long newRuleset(long tenantId) {
        return newRuleset(tenantId, unique("V"), ts("2026-01-01 00:00:00"));
    }

    protected long gtFiscalIdTypeId() {
        return jdbc.queryForObject("SELECT id FROM fiscal_id_type WHERE code = 'NIT'", Long.class);
    }

    protected long newProduct(long tenantId, String sku) {
        return insert("INSERT INTO product (tenant_id, sku, name) VALUES (?, ?, ?)", tenantId, sku, "Product " + sku);
    }

    protected long newPaymentMethod(long tenantId, String code, boolean usesCredit) {
        return insert("INSERT INTO payment_method (tenant_id, code, name, uses_credit) VALUES (?, ?, ?, ?)",
                tenantId, code, code, usesCredit);
    }

    protected long newCustomer(long tenantId, String fiscalId) {
        return insert("INSERT INTO customer (tenant_id, name, fiscal_id, fiscal_id_type_id) VALUES (?, ?, ?, ?)",
                tenantId, "Customer " + fiscalId, fiscalId, gtFiscalIdTypeId());
    }

    protected long newCreditAccount(long customerId, String creditLimit, String available) {
        return insert("INSERT INTO credit_account (customer_id, currency, credit_limit, available) VALUES (?, 'GTQ', ?, ?)",
                customerId, new BigDecimal(creditLimit), new BigDecimal(available));
    }

    protected long newQuote(long tenantId, long customerId, long rulesetId, long paymentMethodId, String total) {
        return newQuoteWithPublicId(UUID.randomUUID(), tenantId, customerId, rulesetId, paymentMethodId, total);
    }

    protected long newQuoteWithPublicId(UUID publicId, long tenantId, long customerId, long rulesetId, long paymentMethodId,
                                        String total) {
        return insert("INSERT INTO quote (public_id, tenant_id, customer_id, ruleset_id, payment_method_id, total, created_at, expires_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                publicId, tenantId, customerId, rulesetId, paymentMethodId, new BigDecimal(total),
                ts("2026-01-01 00:00:00"), ts("2026-01-02 00:00:00"));
    }

    protected long newQuoteLine(long quoteId, long productId, int qty, String unitPrice, String taxableBase,
                                String taxRate, String tax) {
        return insert("INSERT INTO quote_line (quote_id, product_id, qty, unit_price, taxable_base, tax_rate, tax) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                quoteId, productId, qty, new BigDecimal(unitPrice), new BigDecimal(taxableBase),
                new BigDecimal(taxRate), new BigDecimal(tax));
    }

    protected long newSalesOrder(long tenantId, long customerId, long quoteId) {
        return insert("INSERT INTO sales_order (public_number, tenant_id, quote_id, customer_id, created_at) "
                        + "VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID().toString(), tenantId, quoteId, customerId, ts("2026-01-01 00:00:00"));
    }

    protected long newIdempotencyKey(long customerId, String key, long salesOrderId) {
        return insert("INSERT INTO idempotency_key (customer_id, idem_key, sales_order_id) VALUES (?, ?, ?)",
                customerId, key, salesOrderId);
    }

    protected long newLedgerEntry(long creditAccountId, long salesOrderId, String amount, String entryType) {
        return insert("INSERT INTO credit_ledger (credit_account_id, sales_order_id, amount, entry_type, created_at) "
                        + "VALUES (?, ?, ?, ?, ?)",
                creditAccountId, salesOrderId, new BigDecimal(amount), entryType, ts("2026-01-01 00:00:00"));
    }

    protected long newOutbox(long salesOrderId, String destination, String eventType, String status) {
        return insert("INSERT INTO outbox (sales_order_id, destination, event_type, payload, status, created_at) "
                        + "VALUES (?, ?, ?, '{}', ?, ?)",
                salesOrderId, destination, eventType, status, ts("2026-01-01 00:00:00"));
    }
}
