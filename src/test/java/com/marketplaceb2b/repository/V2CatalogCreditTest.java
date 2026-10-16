package com.marketplaceb2b.repository;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class V2CatalogCreditTest extends AbstractDatabaseTest {

    @Test
    void rejectsDuplicateSkuWithinTenant() {
        long tenant = newTenant(unique("Tenant"));
        newProduct(tenant, "SKU-DUP");
        assertRejected("INSERT INTO product (tenant_id, sku, name) VALUES (?, 'SKU-DUP', 'Other')", tenant);
    }

    @Test
    void allowsSameSkuAcrossTenants() {
        long first = newTenant(unique("Tenant"));
        long second = newTenant(unique("Tenant"));
        newProduct(first, "SKU-SHARED");
        assertThatCode(() -> newProduct(second, "SKU-SHARED")).doesNotThrowAnyException();
    }

    @Test
    void rejectsDuplicateProductPricePerVersion() {
        long tenant = newTenant(unique("Tenant"));
        long ruleset = newRuleset(tenant);
        long product = newProduct(tenant, unique("SKU"));
        long category = newTaxCategory(unique("CAT"));
        run("INSERT INTO product_price (ruleset_id, product_id, tax_category_id, unit_price) VALUES (?, ?, ?, 100.00)",
                ruleset, product, category);
        assertRejected("INSERT INTO product_price (ruleset_id, product_id, tax_category_id, unit_price) VALUES (?, ?, ?, 200.00)",
                ruleset, product, category);
    }

    @Test
    void rejectsNegativeUnitPrice() {
        long tenant = newTenant(unique("Tenant"));
        long ruleset = newRuleset(tenant);
        long product = newProduct(tenant, unique("SKU"));
        long category = newTaxCategory(unique("CAT"));
        assertRejected("INSERT INTO product_price (ruleset_id, product_id, tax_category_id, unit_price) VALUES (?, ?, ?, -1.00)",
                ruleset, product, category);
    }

    @Test
    void rejectsDuplicatePaymentCodeWithinTenant() {
        long tenant = newTenant(unique("Tenant"));
        newPaymentMethod(tenant, "PAY-DUP", false);
        assertRejected("INSERT INTO payment_method (tenant_id, code, name, uses_credit) VALUES (?, 'PAY-DUP', 'x', FALSE)", tenant);
    }

    @Test
    void rejectsMissingUsesCredit() {
        long tenant = newTenant(unique("Tenant"));
        assertRejected("INSERT INTO payment_method (tenant_id, code, name, uses_credit) VALUES (?, 'PAYNULL', 'x', NULL)", tenant);
    }

    @Test
    void rejectsCustomerWithUnknownFiscalIdType() {
        long tenant = newTenant(unique("Tenant"));
        assertRejected("INSERT INTO customer (tenant_id, name, fiscal_id, fiscal_id_type_id) VALUES (?, 'C', 'ABC123', 999999)",
                tenant);
    }

    @Test
    void rejectsCustomerWithMissingFiscalId() {
        long tenant = newTenant(unique("Tenant"));
        assertRejected("INSERT INTO customer (tenant_id, name, fiscal_id, fiscal_id_type_id) VALUES (?, 'C', NULL, ?)",
                tenant, gtFiscalIdTypeId());
    }

    @Test
    void rejectsSecondCreditAccountPerCustomer() {
        long tenant = newTenant(unique("Tenant"));
        long customer = newCustomer(tenant, unique("F"));
        newCreditAccount(customer, "100", "50");
        assertRejected("INSERT INTO credit_account (customer_id, currency, credit_limit, available) VALUES (?, 'GTQ', 100, 50)",
                customer);
    }

    @Test
    void rejectsNegativeAvailable() {
        long tenant = newTenant(unique("Tenant"));
        long customer = newCustomer(tenant, unique("F"));
        assertRejected("INSERT INTO credit_account (customer_id, currency, credit_limit, available) VALUES (?, 'GTQ', 100, -1)",
                customer);
    }

    @Test
    void rejectsAvailableAboveLimit() {
        long tenant = newTenant(unique("Tenant"));
        long customer = newCustomer(tenant, unique("F"));
        assertRejected("INSERT INTO credit_account (customer_id, currency, credit_limit, available) VALUES (?, 'GTQ', 100, 150)",
                customer);
    }
}
