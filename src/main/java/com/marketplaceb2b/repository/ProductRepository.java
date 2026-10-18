package com.marketplaceb2b.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.marketplaceb2b.model.Product;

@Repository
public class ProductRepository {

    private static final RowMapper<Product> MAPPER = (rs, rowNum) -> new Product(
            rs.getLong("id"),
            rs.getLong("tenant_id"),
            rs.getString("sku"),
            rs.getString("name"),
            rs.getObject("family_id", Long.class));

    private final PersistenceAccess access;

    public ProductRepository(PersistenceAccess access) {
        this.access = access;
    }

    /** Loads the given products, always scoped by tenant so a caller cannot read another tenant's rows. */
    public List<Product> findByIds(long tenantId, Collection<Long> productIds) {
        if (productIds.isEmpty()) {
            return List.of();
        }
        return access.queryList(
                "SELECT id, tenant_id, sku, name, family_id FROM product "
                        + "WHERE tenant_id = :tenantId AND id IN (:productIds)",
                Map.of("tenantId", tenantId, "productIds", productIds), MAPPER);
    }
}
