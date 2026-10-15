package com.marketplaceb2b.persistence;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class V1ReferenceSchemaTest extends AbstractDatabaseTest {

    @Test
    void rejectsDuplicateCountryCode() {
        run("INSERT INTO country (code, name) VALUES ('T1', 'Testland')");
        assertRejected("INSERT INTO country (code, name) VALUES ('T1', 'Other')");
    }

    @Test
    void rejectsDuplicateFiscalIdTypeCodePerCountry() {
        long country = arCountryId();
        run("INSERT INTO fiscal_id_type (country_id, code, name) VALUES (?, 'NIT', 'NIT')", country);
        assertRejected("INSERT INTO fiscal_id_type (country_id, code, name) VALUES (?, 'NIT', 'Other')", country);
    }

    @Test
    void rejectsDuplicateTenantName() {
        run("INSERT INTO tenant (name, country_id) VALUES ('Tenant Dup', ?)", arCountryId());
        assertRejected("INSERT INTO tenant (name, country_id) VALUES ('Tenant Dup', ?)", arCountryId());
    }

    @Test
    void rejectsDuplicateRulesetVersion() {
        long tenant = newTenant(unique("Tenant"));
        newRuleset(tenant, "V1", ts("2026-01-01 00:00:00"));
        assertRejected("INSERT INTO ruleset (tenant_id, version, currency, currency_scale, rounding_mode, effective_from) "
                + "VALUES (?, 'V1', 'ARS', 2, 'HALF_UP', ?)", tenant, ts("2026-02-01 00:00:00"));
    }

    @Test
    void rejectsDuplicateRulesetEffectiveFrom() {
        long tenant = newTenant(unique("Tenant"));
        newRuleset(tenant, "V1", ts("2026-01-01 00:00:00"));
        assertRejected("INSERT INTO ruleset (tenant_id, version, currency, currency_scale, rounding_mode, effective_from) "
                + "VALUES (?, 'V2', 'ARS', 2, 'HALF_UP', ?)", tenant, ts("2026-01-01 00:00:00"));
    }

    @Test
    void rejectsTaxRateOutOfRange() {
        long ruleset = newRuleset(newTenant(unique("Tenant")));
        long category = newTaxCategory(unique("CAT"));
        assertRejected("INSERT INTO tax_rate (ruleset_id, tax_category_id, rate) VALUES (?, ?, 150.0)", ruleset, category);
    }

    @Test
    void rejectsDuplicateTaxRatePerCategory() {
        long ruleset = newRuleset(newTenant(unique("Tenant")));
        long category = newTaxCategory(unique("CAT"));
        run("INSERT INTO tax_rate (ruleset_id, tax_category_id, rate) VALUES (?, ?, 21.0)", ruleset, category);
        assertRejected("INSERT INTO tax_rate (ruleset_id, tax_category_id, rate) VALUES (?, ?, 10.5)", ruleset, category);
    }

    @Test
    void rejectsForeignKeyToUnknownParent() {
        assertRejected("INSERT INTO tenant (name, country_id) VALUES (?, ?)", unique("Tenant"), 999999L);
    }

    @Test
    void acceptsValidReferenceRow() {
        assertThatCode(() -> newTaxCategory(unique("CAT"))).doesNotThrowAnyException();
    }
}
