package com.marketplaceb2b.persistence.repository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.marketplaceb2b.model.CreditAccount;
import com.marketplaceb2b.persistence.PersistenceAccess;

@Repository
public class CreditAccountRepository {

    private static final RowMapper<CreditAccount> MAPPER = (rs, rowNum) -> new CreditAccount(
            rs.getLong("id"),
            rs.getLong("customer_id"),
            rs.getString("currency"),
            rs.getBigDecimal("credit_limit"),
            rs.getBigDecimal("available"));

    private final PersistenceAccess access;

    public CreditAccountRepository(PersistenceAccess access) {
        this.access = access;
    }

    public Optional<CreditAccount> lockByCustomer(long customerId) {
        return access.queryList(
                "SELECT id, customer_id, currency, credit_limit, available FROM credit_account "
                        + "WHERE customer_id = :customerId FOR UPDATE",
                Map.of("customerId", customerId), MAPPER).stream().findFirst();
    }

    public void consume(long creditAccountId, BigDecimal amount) {
        access.insert("UPDATE credit_account SET available = available - :amount WHERE id = :id",
                Map.of("amount", amount, "id", creditAccountId));
    }
}
