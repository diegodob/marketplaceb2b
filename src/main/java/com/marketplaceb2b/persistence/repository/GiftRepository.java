package com.marketplaceb2b.persistence.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.marketplaceb2b.model.Gift;
import com.marketplaceb2b.model.GiftCondition;
import com.marketplaceb2b.persistence.PersistenceAccess;

@Repository
public class GiftRepository {

    private static final RowMapper<GiftRow> ROW = (rs, rowNum) -> new GiftRow(
            rs.getLong("id"),
            rs.getLong("ruleset_id"),
            rs.getLong("condition_id"),
            rs.getLong("gift_product_id"),
            rs.getInt("gift_qty"),
            rs.getBoolean("taxable"));

    private static final RowMapper<GiftCondition> CONDITION = (rs, rowNum) -> new GiftCondition(
            rs.getObject("product_id", Long.class),
            rs.getObject("family_id", Long.class),
            rs.getObject("min_qty", Integer.class),
            rs.getBigDecimal("min_amount"));

    private final PersistenceAccess access;

    public GiftRepository(PersistenceAccess access) {
        this.access = access;
    }

    public List<Gift> findByRuleset(long rulesetId) {
        List<GiftRow> rows = access.queryList(
                "SELECT id, ruleset_id, condition_id, gift_product_id, gift_qty, taxable FROM gift "
                        + "WHERE ruleset_id = :rulesetId ORDER BY id",
                Map.of("rulesetId", rulesetId), ROW);

        List<Gift> gifts = new ArrayList<>();
        for (GiftRow row : rows) {
            GiftCondition condition = access.query(
                    "SELECT product_id, family_id, min_qty, min_amount FROM gift_condition WHERE id = :id",
                    Map.of("id", row.conditionId()), CONDITION);
            gifts.add(new Gift(row.id(), row.rulesetId(), condition, row.giftProductId(), row.giftQty(),
                    row.taxable()));
        }
        return gifts;
    }

    private record GiftRow(long id, long rulesetId, long conditionId, long giftProductId, int giftQty,
                           boolean taxable) {
    }
}
