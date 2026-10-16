package com.marketplaceb2b.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ForeignKeyIndexTest extends AbstractDatabaseTest {

    private static final List<String> TABLES = List.of(
            "fiscal_id_type", "tenant", "ruleset", "tax_rate", "product_family", "product", "product_price",
            "payment_method", "customer", "credit_account", "quote", "quote_line", "sales_order",
            "idempotency_key", "credit_ledger", "outbox",
            "scale_discount", "scale_tier", "combo", "combo_item", "gift_condition", "gift");

    @Test
    void everyForeignKeyColumnHasALeadingIndex() throws SQLException {
        List<String> missing = new ArrayList<>();
        try (Connection connection = jdbc.getDataSource().getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            for (String table : TABLES) {
                for (String column : foreignKeyColumns(metaData, table)) {
                    if (!hasLeadingIndex(metaData, table, column)) {
                        missing.add(table + "." + column);
                    }
                }
            }
        }
        assertThat(missing).isEmpty();
    }

    private Set<String> foreignKeyColumns(DatabaseMetaData metaData, String table) throws SQLException {
        Set<String> columns = new LinkedHashSet<>();
        try (ResultSet rs = metaData.getImportedKeys(null, null, table)) {
            while (rs.next()) {
                columns.add(rs.getString("FKCOLUMN_NAME").toLowerCase());
            }
        }
        return columns;
    }

    private boolean hasLeadingIndex(DatabaseMetaData metaData, String table, String column) throws SQLException {
        for (boolean unique : new boolean[] {false, true}) {
            try (ResultSet rs = metaData.getIndexInfo(null, null, table, unique, false)) {
                while (rs.next()) {
                    String indexedColumn = rs.getString("COLUMN_NAME");
                    if (indexedColumn != null && rs.getShort("ORDINAL_POSITION") == 1
                            && indexedColumn.equalsIgnoreCase(column)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
