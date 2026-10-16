package com.marketplaceb2b.persistence;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class LeverSchemaTest extends AbstractDatabaseTest {

    private long newScale(long rulesetId, long productId) {
        return insert("INSERT INTO scale_discount (ruleset_id, target_type, product_id, family_id) VALUES (?, 'SKU', ?, NULL)",
                rulesetId, productId);
    }

    private long newCombo(long rulesetId) {
        return insert("INSERT INTO combo (ruleset_id, name, combo_price) VALUES (?, ?, 100.00)", rulesetId,
                unique("Combo"));
    }

    @Test
    void scaleAcceptsSkuTarget() {
        long tenant = newTenant(unique("Tenant"));
        long ruleset = newRuleset(tenant);
        long product = newProduct(tenant, unique("SKU"));
        assertThatCode(() -> newScale(ruleset, product)).doesNotThrowAnyException();
    }

    @Test
    void scaleRejectsSkuTargetWithoutProduct() {
        long ruleset = newRuleset(newTenant(unique("Tenant")));
        assertRejected("INSERT INTO scale_discount (ruleset_id, target_type, product_id, family_id) VALUES (?, 'SKU', NULL, NULL)",
                ruleset);
    }

    @Test
    void scaleRejectsSkuTargetWithFamily() {
        long tenant = newTenant(unique("Tenant"));
        long ruleset = newRuleset(tenant);
        long product = newProduct(tenant, unique("SKU"));
        long family = insert("INSERT INTO product_family (tenant_id, name) VALUES (?, ?)", tenant, unique("Fam"));
        assertRejected("INSERT INTO scale_discount (ruleset_id, target_type, product_id, family_id) VALUES (?, 'SKU', ?, ?)",
                ruleset, product, family);
    }

    @Test
    void scaleRejectsUnknownTargetType() {
        long tenant = newTenant(unique("Tenant"));
        long ruleset = newRuleset(tenant);
        long product = newProduct(tenant, unique("SKU"));
        assertRejected("INSERT INTO scale_discount (ruleset_id, target_type, product_id, family_id) VALUES (?, 'BOTH', ?, NULL)",
                ruleset, product);
    }

    @Test
    void tierRejectsNonPositiveStart() {
        long tenant = newTenant(unique("Tenant"));
        long scale = newScale(newRuleset(tenant), newProduct(tenant, unique("SKU")));
        assertRejected("INSERT INTO scale_tier (scale_discount_id, from_qty, percentage) VALUES (?, 0, 5.0)", scale);
    }

    @Test
    void tierRejectsOutOfRangePercentage() {
        long tenant = newTenant(unique("Tenant"));
        long scale = newScale(newRuleset(tenant), newProduct(tenant, unique("SKU")));
        assertRejected("INSERT INTO scale_tier (scale_discount_id, from_qty, percentage) VALUES (?, 5, 150.0)", scale);
    }

    @Test
    void tierAcceptsNonIncreasingOrder() {
        long tenant = newTenant(unique("Tenant"));
        long scale = newScale(newRuleset(tenant), newProduct(tenant, unique("SKU")));
        assertThatCode(() -> {
            run("INSERT INTO scale_tier (scale_discount_id, from_qty, percentage) VALUES (?, 50, 5.0)", scale);
            run("INSERT INTO scale_tier (scale_discount_id, from_qty, percentage) VALUES (?, 10, 10.0)", scale);
        }).doesNotThrowAnyException();
    }

    @Test
    void comboRejectsNegativePrice() {
        long ruleset = newRuleset(newTenant(unique("Tenant")));
        assertRejected("INSERT INTO combo (ruleset_id, name, combo_price) VALUES (?, 'c', -1.00)", ruleset);
    }

    @Test
    void comboItemRejectsNonPositiveMinQty() {
        long tenant = newTenant(unique("Tenant"));
        long combo = newCombo(newRuleset(tenant));
        long product = newProduct(tenant, unique("SKU"));
        assertRejected("INSERT INTO combo_item (combo_id, product_id, min_qty) VALUES (?, ?, 0)", combo, product);
    }

    @Test
    void comboItemRejectsDuplicateProduct() {
        long tenant = newTenant(unique("Tenant"));
        long combo = newCombo(newRuleset(tenant));
        long product = newProduct(tenant, unique("SKU"));
        run("INSERT INTO combo_item (combo_id, product_id, min_qty) VALUES (?, ?, 6)", combo, product);
        assertRejected("INSERT INTO combo_item (combo_id, product_id, min_qty) VALUES (?, ?, 3)", combo, product);
    }

    @Test
    void giftConditionRejectsNoThreshold() {
        assertRejected("INSERT INTO gift_condition (product_id, family_id, min_qty, min_amount) VALUES (NULL, NULL, NULL, NULL)");
    }

    @Test
    void giftConditionRejectsQuantityWithoutTarget() {
        assertRejected("INSERT INTO gift_condition (product_id, family_id, min_qty, min_amount) VALUES (NULL, NULL, 5, NULL)");
    }

    @Test
    void giftConditionRejectsBothTargets() {
        long tenant = newTenant(unique("Tenant"));
        long product = newProduct(tenant, unique("SKU"));
        long family = insert("INSERT INTO product_family (tenant_id, name) VALUES (?, ?)", tenant, unique("Fam"));
        assertRejected("INSERT INTO gift_condition (product_id, family_id, min_qty, min_amount) VALUES (?, ?, 5, NULL)",
                product, family);
    }

    @Test
    void giftConditionRejectsNegativeAmount() {
        assertRejected("INSERT INTO gift_condition (product_id, family_id, min_qty, min_amount) VALUES (NULL, NULL, NULL, -1.00)");
    }

    @Test
    void giftConditionAcceptsAmountOnly() {
        assertThatCode(() -> run("INSERT INTO gift_condition (product_id, family_id, min_qty, min_amount) VALUES (NULL, NULL, NULL, 1000.00)"))
                .doesNotThrowAnyException();
    }

    @Test
    void giftRejectsNonPositiveQuantity() {
        long tenant = newTenant(unique("Tenant"));
        long ruleset = newRuleset(tenant);
        long product = newProduct(tenant, unique("SKU"));
        long condition = insert("INSERT INTO gift_condition (product_id, family_id, min_qty, min_amount) VALUES (?, NULL, 5, NULL)",
                product);
        assertRejected("INSERT INTO gift (ruleset_id, condition_id, gift_product_id, gift_qty) VALUES (?, ?, ?, 0)",
                ruleset, condition, product);
    }
}
