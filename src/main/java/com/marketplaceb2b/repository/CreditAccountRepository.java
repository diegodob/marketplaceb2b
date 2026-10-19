package com.marketplaceb2b.repository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.marketplaceb2b.model.CreditAccount;

@Repository
public class CreditAccountRepository {

    private static final RowMapper<CreditAccount> MAPPER = (rs, rowNum) -> new CreditAccount(
            rs.getLong("id"),
            rs.getLong("customer_id"),
            rs.getString("currency"),
            rs.getBigDecimal("credit_limit"),
            rs.getBigDecimal("available"));

    private static final String SELECT = "SELECT id, customer_id, currency, credit_limit, available "
            + "FROM credit_account WHERE customer_id = :customerId "
            + "AND customer_id IN (SELECT id FROM customer WHERE tenant_id = :tenantId)";

    private final PersistenceAccess access;

    public CreditAccountRepository(PersistenceAccess access) {
        this.access = access;
    }

    /** Reads the customer's account scoped by tenant, without locking (a pricing preview). */
    public Optional<CreditAccount> findByCustomerAndTenant(long customerId, long tenantId) {
        return access.queryList(SELECT, Map.of("customerId", customerId, "tenantId", tenantId), MAPPER)
                .stream().findFirst();
    }

    /** Locks the customer's account scoped by tenant, for the confirmation that consumes it. */
    public Optional<CreditAccount> lockByCustomerAndTenant(long customerId, long tenantId) {
        return access.queryList(SELECT + " FOR UPDATE", Map.of("customerId", customerId, "tenantId", tenantId),
                MAPPER).stream().findFirst();
    }

    public void consume(long creditAccountId, BigDecimal amount) {
        access.insert("UPDATE credit_account SET available = available - :amount WHERE id = :id",
                Map.of("amount", amount, "id", creditAccountId));
    }
}
