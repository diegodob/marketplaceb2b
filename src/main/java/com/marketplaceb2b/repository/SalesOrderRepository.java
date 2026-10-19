package com.marketplaceb2b.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.marketplaceb2b.model.SalesOrder;

@Repository
public class SalesOrderRepository {

    private static final RowMapper<SalesOrder> MAPPER = (rs, rowNum) -> {
        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        return new SalesOrder(
                rs.getLong("id"),
                rs.getString("public_number"),
                rs.getLong("tenant_id"),
                rs.getLong("quote_id"),
                rs.getLong("customer_id"),
                rs.getTimestamp("created_at").toInstant(),
                deletedAt == null ? null : deletedAt.toInstant());
    };

    private final PersistenceAccess access;

    public SalesOrderRepository(PersistenceAccess access) {
        this.access = access;
    }

    public long insert(long tenantId, long quoteId, long customerId, String publicNumber, Instant createdAt) {
        return access.insertReturningId(
                "INSERT INTO sales_order (public_number, tenant_id, quote_id, customer_id, created_at) "
                        + "VALUES (:publicNumber, :tenantId, :quoteId, :customerId, :createdAt)",
                Map.of("publicNumber", publicNumber, "tenantId", tenantId, "quoteId", quoteId,
                        "customerId", customerId, "createdAt", Timestamp.from(createdAt)));
    }

    /** Reads the order scoped by tenant, so another tenant's id never resolves. */
    public Optional<SalesOrder> findById(long tenantId, long id) {
        return access.queryList(
                "SELECT id, public_number, tenant_id, quote_id, customer_id, created_at, deleted_at "
                        + "FROM sales_order WHERE tenant_id = :tenantId AND id = :id",
                Map.of("tenantId", tenantId, "id", id), MAPPER).stream().findFirst();
    }

    /** Reads the order by the client's public number, scoped by tenant. */
    public Optional<SalesOrder> findByPublicNumber(long tenantId, String publicNumber) {
        return access.queryList(
                "SELECT id, public_number, tenant_id, quote_id, customer_id, created_at, deleted_at "
                        + "FROM sales_order WHERE tenant_id = :tenantId AND public_number = :publicNumber",
                Map.of("tenantId", tenantId, "publicNumber", publicNumber), MAPPER).stream().findFirst();
    }

    /** Finds the order a quote already produced, scoped by tenant (one order per quote). */
    public Optional<SalesOrder> findByQuote(long tenantId, long quoteId) {
        return access.queryList(
                "SELECT id, public_number, tenant_id, quote_id, customer_id, created_at, deleted_at "
                        + "FROM sales_order WHERE tenant_id = :tenantId AND quote_id = :quoteId",
                Map.of("tenantId", tenantId, "quoteId", quoteId), MAPPER).stream().findFirst();
    }
}
