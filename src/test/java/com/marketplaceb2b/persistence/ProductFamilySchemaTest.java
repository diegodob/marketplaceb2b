package com.marketplaceb2b.persistence;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class ProductFamilySchemaTest extends AbstractDatabaseTest {

    @Test
    void rejectsDuplicateFamilyNameWithinTenant() {
        long tenant = newTenant(unique("Tenant"));
        run("INSERT INTO product_family (tenant_id, name) VALUES (?, 'Gaseosas')", tenant);
        assertRejected("INSERT INTO product_family (tenant_id, name) VALUES (?, 'Gaseosas')", tenant);
    }

    @Test
    void allowsSameFamilyNameAcrossTenants() {
        long first = newTenant(unique("Tenant"));
        long second = newTenant(unique("Tenant"));
        run("INSERT INTO product_family (tenant_id, name) VALUES (?, 'Gaseosas')", first);
        assertThatCode(() -> run("INSERT INTO product_family (tenant_id, name) VALUES (?, 'Gaseosas')", second))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsProductWithUnknownFamily() {
        long tenant = newTenant(unique("Tenant"));
        assertRejected("INSERT INTO product (tenant_id, sku, name, family_id) VALUES (?, ?, 'P', 999999)",
                tenant, unique("SKU"));
    }

    @Test
    void acceptsProductWithExistingFamily() {
        long tenant = newTenant(unique("Tenant"));
        long family = insert("INSERT INTO product_family (tenant_id, name) VALUES (?, ?)", tenant, unique("Fam"));
        assertThatCode(() -> run("INSERT INTO product (tenant_id, sku, name, family_id) VALUES (?, ?, 'P', ?)",
                tenant, unique("SKU"), family)).doesNotThrowAnyException();
    }
}
