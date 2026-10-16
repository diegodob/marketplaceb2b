package com.marketplaceb2b.repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.marketplaceb2b.model.Combo;
import com.marketplaceb2b.model.ComboItem;

@Repository
public class ComboRepository {

    private static final RowMapper<ComboRow> ROW = (rs, rowNum) -> new ComboRow(
            rs.getLong("id"),
            rs.getLong("ruleset_id"),
            rs.getString("name"),
            rs.getBigDecimal("combo_price"),
            rs.getInt("priority"));

    private static final RowMapper<ComboItem> ITEM = (rs, rowNum) -> new ComboItem(
            rs.getLong("product_id"),
            rs.getInt("min_qty"));

    private final PersistenceAccess access;

    public ComboRepository(PersistenceAccess access) {
        this.access = access;
    }

    public List<Combo> findByRuleset(long rulesetId) {
        List<ComboRow> rows = access.queryList(
                "SELECT id, ruleset_id, name, combo_price, priority FROM combo WHERE ruleset_id = :rulesetId ORDER BY id",
                Map.of("rulesetId", rulesetId), ROW);

        List<Combo> combos = new ArrayList<>();
        for (ComboRow row : rows) {
            List<ComboItem> items = access.queryList(
                    "SELECT product_id, min_qty FROM combo_item WHERE combo_id = :id ORDER BY id",
                    Map.of("id", row.id()), ITEM);
            combos.add(new Combo(row.id(), row.rulesetId(), row.name(), items, row.comboPrice(), row.priority()));
        }
        return combos;
    }

    private record ComboRow(long id, long rulesetId, String name, BigDecimal comboPrice, int priority) {
    }
}
