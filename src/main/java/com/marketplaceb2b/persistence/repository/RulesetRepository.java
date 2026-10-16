package com.marketplaceb2b.persistence.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.marketplaceb2b.model.RoundingMode;
import com.marketplaceb2b.model.Ruleset;
import com.marketplaceb2b.persistence.PersistenceAccess;

@Repository
public class RulesetRepository {

    private static final RowMapper<Ruleset> MAPPER = (rs, rowNum) -> new Ruleset(
            rs.getLong("id"),
            rs.getLong("tenant_id"),
            rs.getString("version"),
            rs.getString("currency"),
            rs.getInt("currency_scale"),
            RoundingMode.valueOf(rs.getString("rounding_mode")),
            rs.getTimestamp("effective_from").toInstant());

    private final PersistenceAccess access;

    public RulesetRepository(PersistenceAccess access) {
        this.access = access;
    }

    public Optional<Ruleset> currentForTenant(long tenantId, Instant at) {
        return access.queryList(
                "SELECT id, tenant_id, version, currency, currency_scale, rounding_mode, effective_from FROM ruleset "
                        + "WHERE tenant_id = :tenantId AND effective_from <= :at ORDER BY effective_from DESC",
                Map.of("tenantId", tenantId, "at", Timestamp.from(at)), MAPPER).stream().findFirst();
    }
}
