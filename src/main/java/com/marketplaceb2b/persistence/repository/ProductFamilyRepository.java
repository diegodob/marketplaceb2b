package com.marketplaceb2b.persistence.repository;

import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.marketplaceb2b.model.ProductFamily;
import com.marketplaceb2b.persistence.PersistenceAccess;

@Repository
public class ProductFamilyRepository {

    private static final RowMapper<ProductFamily> MAPPER = (rs, rowNum) -> new ProductFamily(
            rs.getLong("id"),
            rs.getLong("tenant_id"),
            rs.getString("name"));

    private final PersistenceAccess access;

    public ProductFamilyRepository(PersistenceAccess access) {
        this.access = access;
    }

    public Optional<ProductFamily> find(long tenantId, long id) {
        return access.queryList(
                "SELECT id, tenant_id, name FROM product_family WHERE tenant_id = :tenantId AND id = :id",
                Map.of("tenantId", tenantId, "id", id), MAPPER).stream().findFirst();
    }

    public long insert(long tenantId, String name) {
        return access.insertReturningId(
                "INSERT INTO product_family (tenant_id, name) VALUES (:tenantId, :name)",
                Map.of("tenantId", tenantId, "name", name));
    }
}
