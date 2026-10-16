package com.marketplaceb2b.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.marketplaceb2b.model.DiscountTargetType;
import com.marketplaceb2b.model.ScaleDiscount;
import com.marketplaceb2b.model.ScaleTier;

@Repository
public class ScaleDiscountRepository {

    private static final RowMapper<ScaleRow> ROW = (rs, rowNum) -> new ScaleRow(
            rs.getLong("id"),
            rs.getLong("ruleset_id"),
            DiscountTargetType.valueOf(rs.getString("target_type")),
            rs.getObject("product_id", Long.class),
            rs.getObject("family_id", Long.class));

    private static final RowMapper<ScaleTier> TIER = (rs, rowNum) -> new ScaleTier(
            rs.getInt("from_qty"),
            rs.getBigDecimal("percentage"));

    private final PersistenceAccess access;

    public ScaleDiscountRepository(PersistenceAccess access) {
        this.access = access;
    }

    public List<ScaleDiscount> findByRuleset(long rulesetId) {
        List<ScaleRow> rows = access.queryList(
                "SELECT id, ruleset_id, target_type, product_id, family_id FROM scale_discount "
                        + "WHERE ruleset_id = :rulesetId ORDER BY id",
                Map.of("rulesetId", rulesetId), ROW);

        List<ScaleDiscount> scales = new ArrayList<>();
        for (ScaleRow row : rows) {
            List<ScaleTier> tiers = access.queryList(
                    "SELECT from_qty, percentage FROM scale_tier WHERE scale_discount_id = :id ORDER BY from_qty",
                    Map.of("id", row.id()), TIER);
            scales.add(new ScaleDiscount(row.id(), row.rulesetId(), row.targetType(), row.productId(), row.familyId(),
                    tiers));
        }
        return scales;
    }

    private record ScaleRow(long id, long rulesetId, DiscountTargetType targetType, Long productId, Long familyId) {
    }
}
