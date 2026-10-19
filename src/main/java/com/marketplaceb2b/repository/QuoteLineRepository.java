package com.marketplaceb2b.repository;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.marketplaceb2b.model.QuoteLine;

@Repository
public class QuoteLineRepository {

    private static final RowMapper<QuoteLine> MAPPER = (rs, rowNum) -> new QuoteLine(
            rs.getLong("id"),
            rs.getLong("quote_id"),
            rs.getLong("product_id"),
            rs.getInt("qty"),
            rs.getBigDecimal("unit_price"),
            rs.getBigDecimal("taxable_base"),
            rs.getBigDecimal("tax_rate"),
            rs.getBigDecimal("tax"));

    private final PersistenceAccess access;

    public QuoteLineRepository(PersistenceAccess access) {
        this.access = access;
    }

    /** Reads the quote's lines scoped by tenant through the quote. */
    public List<QuoteLine> findByQuote(long tenantId, long quoteId) {
        return access.queryList(
                "SELECT l.id, l.quote_id, l.product_id, l.qty, l.unit_price, l.taxable_base, l.tax_rate, l.tax "
                        + "FROM quote_line l JOIN quote q ON q.id = l.quote_id "
                        + "WHERE q.tenant_id = :tenantId AND l.quote_id = :quoteId ORDER BY l.product_id",
                Map.of("tenantId", tenantId, "quoteId", quoteId), MAPPER);
    }
}
