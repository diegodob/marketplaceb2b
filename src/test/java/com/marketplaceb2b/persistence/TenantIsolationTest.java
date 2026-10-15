package com.marketplaceb2b.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class TenantIsolationTest extends AbstractDatabaseTest {

    @Test
    void sameSkuInTwoTenantsIsNotLeaked() {
        long tenantA = newTenant(unique("TenantA"));
        long tenantB = newTenant(unique("TenantB"));
        newProduct(tenantA, "SKU-ISO");
        newProduct(tenantB, "SKU-ISO");

        List<Long> productsForA = jdbc.query(
                "SELECT id FROM product WHERE tenant_id = ? AND sku = 'SKU-ISO'",
                (rs, rowNum) -> rs.getLong(1), tenantA);

        assertThat(productsForA).hasSize(1);
        Long owner = jdbc.queryForObject("SELECT tenant_id FROM product WHERE id = ?", Long.class, productsForA.get(0));
        assertThat(owner).isEqualTo(tenantA);
    }
}
