package com.marketplaceb2b.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.marketplaceb2b.model.ProductPrice;

@Repository
public class ProductPriceRepository {

    private static final RowMapper<ProductPrice> MAPPER = (rs, rowNum) -> new ProductPrice(
            rs.getLong("id"),
            rs.getLong("ruleset_id"),
            rs.getLong("product_id"),
            rs.getLong("tax_category_id"),
            rs.getBigDecimal("unit_price"));

    private final PersistenceAccess access;

    public ProductPriceRepository(PersistenceAccess access) {
        this.access = access;
    }

    public List<ProductPrice> findByRulesetAndProducts(long rulesetId, Collection<Long> productIds) {
        if (productIds.isEmpty()) {
            return List.of();
        }
        return access.queryList(
                "SELECT id, ruleset_id, product_id, tax_category_id, unit_price FROM product_price "
                        + "WHERE ruleset_id = :rulesetId AND product_id IN (:productIds)",
                Map.of("rulesetId", rulesetId, "productIds", productIds), MAPPER);
    }
}
