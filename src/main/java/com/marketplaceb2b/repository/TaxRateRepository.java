package com.marketplaceb2b.repository;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.marketplaceb2b.model.TaxRate;

@Repository
public class TaxRateRepository {

    private static final RowMapper<TaxRate> MAPPER = (rs, rowNum) -> new TaxRate(
            rs.getLong("id"),
            rs.getLong("ruleset_id"),
            rs.getLong("tax_category_id"),
            rs.getBigDecimal("rate"));

    private final PersistenceAccess access;

    public TaxRateRepository(PersistenceAccess access) {
        this.access = access;
    }

    public List<TaxRate> findByRuleset(long rulesetId) {
        return access.queryList(
                "SELECT id, ruleset_id, tax_category_id, rate FROM tax_rate WHERE ruleset_id = :rulesetId",
                Map.of("rulesetId", rulesetId), MAPPER);
    }
}
