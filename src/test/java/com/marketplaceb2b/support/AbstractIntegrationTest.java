package com.marketplaceb2b.support;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
public abstract class AbstractIntegrationTest {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    @Autowired
    protected JdbcTemplate jdbc;

    protected static String unique(String prefix) {
        return prefix + COUNTER.incrementAndGet();
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

    protected long gtTenantId() {
        return jdbc.queryForObject("SELECT id FROM tenant WHERE name = 'Bebidas GT'", Long.class);
    }

    protected long newTenant(String name) {
        return insert("INSERT INTO tenant (name, country_id) VALUES (?, (SELECT id FROM country WHERE code = 'GT'))",
                name);
    }

    protected long gtCustomerId() {
        return jdbc.queryForObject("SELECT id FROM customer WHERE name = 'Cliente 12'", Long.class);
    }

    protected long gtRulesetId() {
        return jdbc.queryForObject("SELECT id FROM ruleset WHERE version = 'GT-2026-10-01.1'", Long.class);
    }

    protected long paymentMethodId(String code) {
        return jdbc.queryForObject("SELECT id FROM payment_method WHERE code = ?", Long.class, code);
    }

    protected long newQuote(String total, Instant expiresAt) {
        return newQuote(gtTenantId(), gtCustomerId(), gtRulesetId(), paymentMethodId("CREDIT"), total, expiresAt);
    }

    protected long newQuote(long tenantId, long customerId, long rulesetId, long paymentMethodId, String total,
                            Instant expiresAt) {
        return insert("INSERT INTO quote (public_id, tenant_id, customer_id, ruleset_id, payment_method_id, total, created_at, expires_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), tenantId, customerId, rulesetId, paymentMethodId, new BigDecimal(total),
                Timestamp.from(Instant.parse("2026-01-01T00:00:00Z")), Timestamp.from(expiresAt));
    }
}
