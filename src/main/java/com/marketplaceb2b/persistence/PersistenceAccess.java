package com.marketplaceb2b.persistence;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PersistenceAccess {

    private static final Set<String> APPEND_ONLY_TABLES = Set.of("ruleset", "quote", "quote_line", "credit_ledger");

    private final NamedParameterJdbcTemplate jdbc;

    public PersistenceAccess(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(String sql, Map<String, ?> params) {
        assertNotMutatingAppendOnly(sql);
        return jdbc.update(sql, params);
    }

    public <T> T query(String sql, Map<String, ?> params, RowMapper<T> rowMapper) {
        return jdbc.queryForObject(sql, params, rowMapper);
    }

    public <T> List<T> queryList(String sql, Map<String, ?> params, RowMapper<T> rowMapper) {
        return jdbc.query(sql, params, rowMapper);
    }

    static void assertNotMutatingAppendOnly(String sql) {
        String normalized = sql.stripLeading().toLowerCase(Locale.ROOT);
        for (String table : APPEND_ONLY_TABLES) {
            if (normalized.startsWith("update " + table) || normalized.startsWith("delete from " + table)) {
                throw new UnsupportedOperationException(
                        "Table '" + table + "' is append-only; insert a compensating REVERSAL row instead");
            }
        }
    }
}
