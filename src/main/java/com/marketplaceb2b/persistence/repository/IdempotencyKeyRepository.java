package com.marketplaceb2b.persistence.repository;

import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.marketplaceb2b.model.IdempotencyKey;
import com.marketplaceb2b.persistence.PersistenceAccess;

@Repository
public class IdempotencyKeyRepository {

    private static final RowMapper<IdempotencyKey> MAPPER = (rs, rowNum) -> new IdempotencyKey(
            rs.getLong("id"),
            rs.getLong("customer_id"),
            rs.getString("idem_key"),
            rs.getLong("sales_order_id"));

    private final PersistenceAccess access;

    public IdempotencyKeyRepository(PersistenceAccess access) {
        this.access = access;
    }

    public Optional<IdempotencyKey> find(long customerId, String key) {
        return access.queryList(
                "SELECT id, customer_id, idem_key, sales_order_id FROM idempotency_key "
                        + "WHERE customer_id = :customerId AND idem_key = :key",
                Map.of("customerId", customerId, "key", key), MAPPER).stream().findFirst();
    }

    public void insert(long customerId, String key, long salesOrderId) {
        access.insert("INSERT INTO idempotency_key (customer_id, idem_key, sales_order_id) "
                        + "VALUES (:customerId, :key, :salesOrderId)",
                Map.of("customerId", customerId, "key", key, "salesOrderId", salesOrderId));
    }
}
