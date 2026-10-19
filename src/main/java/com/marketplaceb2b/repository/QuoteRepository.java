package com.marketplaceb2b.repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.marketplaceb2b.model.Quote;

@Repository
public class QuoteRepository {

    private static final RowMapper<Quote> MAPPER = (rs, rowNum) -> new Quote(
            rs.getLong("id"),
            rs.getObject("public_id", UUID.class),
            rs.getLong("tenant_id"),
            rs.getLong("customer_id"),
            rs.getLong("ruleset_id"),
            rs.getLong("payment_method_id"),
            rs.getBigDecimal("total"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("expires_at").toInstant());

    private final PersistenceAccess access;

    public QuoteRepository(PersistenceAccess access) {
        this.access = access;
    }

    public Optional<Quote> find(long tenantId, long quoteId) {
        return access.queryList(
                "SELECT id, public_id, tenant_id, customer_id, ruleset_id, payment_method_id, total, created_at, expires_at "
                        + "FROM quote WHERE tenant_id = :tenantId AND id = :quoteId",
                Map.of("tenantId", tenantId, "quoteId", quoteId), MAPPER).stream().findFirst();
    }

    /** Resolves the client's reference scoped by tenant, so another tenant's quote never resolves. */
    public Optional<Quote> findByPublicId(long tenantId, UUID publicId) {
        return access.queryList(
                "SELECT id, public_id, tenant_id, customer_id, ruleset_id, payment_method_id, total, created_at, expires_at "
                        + "FROM quote WHERE tenant_id = :tenantId AND public_id = :publicId",
                Map.of("tenantId", tenantId, "publicId", publicId), MAPPER).stream().findFirst();
    }
}
