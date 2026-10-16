package com.marketplaceb2b.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.marketplaceb2b.dto.Cart;
import com.marketplaceb2b.dto.CartLine;
import com.marketplaceb2b.dto.DiscountBreakdown;
import com.marketplaceb2b.dto.DiscountOrigin;
import com.marketplaceb2b.dto.LineBreakdown;
import com.marketplaceb2b.dto.PriceBreakdown;
import com.marketplaceb2b.support.AbstractIntegrationTest;

class ComboPricingTest extends AbstractIntegrationTest {

    @Autowired
    private OrderPricingService service;

    @Test
    void appliesTheSeededComboToTheCoveredMinimums() {
        PriceBreakdown breakdown = service.price(guatemalaCart(map("GAS-500", 6, "AGU-500", 6)));

        LineBreakdown gas = line(breakdown, product(gtTenantId(), "GAS-500"));
        LineBreakdown agu = line(breakdown, product(gtTenantId(), "AGU-500"));

        assertThat(gas.gross()).isEqualByComparingTo("24000.00");
        assertThat(gas.discount()).isEqualByComparingTo("11692.31");
        assertThat(gas.taxableBase()).isEqualByComparingTo("12307.69");
        assertThat(gas.tax()).isEqualByComparingTo("1476.92");

        assertThat(agu.gross()).isEqualByComparingTo("15000.00");
        assertThat(agu.discount()).isEqualByComparingTo("7307.69");
        assertThat(agu.taxableBase()).isEqualByComparingTo("7692.31");
        assertThat(agu.tax()).isEqualByComparingTo("923.08");

        assertThat(breakdown.grossSubtotal()).isEqualByComparingTo("39000.00");
        assertThat(breakdown.totalDiscounts()).isEqualByComparingTo("19000.00");
        assertThat(breakdown.totalTaxableBase()).isEqualByComparingTo("20000.00");
        assertThat(breakdown.totalTax()).isEqualByComparingTo("2400.00");
        assertThat(breakdown.total()).isEqualByComparingTo("22400.00");
    }

    @Test
    void appliesAComboOfTwoProductsWithDifferentTaxRates() {
        clearCombos(gtRulesetId());
        long comboId = insertCombo(gtRulesetId(), "Gaseosa + Leche", "35000", 20, map("GAS-500", 6, "LEC-1L", 3));

        PriceBreakdown breakdown = service.price(guatemalaCart(map("GAS-500", 6, "LEC-1L", 3)));

        LineBreakdown gas = line(breakdown, product(gtTenantId(), "GAS-500"));
        LineBreakdown lec = line(breakdown, product(gtTenantId(), "LEC-1L"));

        assertThat(gas.gross()).isEqualByComparingTo("24000.00");
        assertThat(gas.discount()).isEqualByComparingTo("4000.00");
        assertThat(gas.taxableBase()).isEqualByComparingTo("20000.00");
        assertThat(gas.taxRate()).isEqualByComparingTo("12.0000");
        assertThat(gas.tax()).isEqualByComparingTo("2400.00");

        assertThat(lec.gross()).isEqualByComparingTo("18000.00");
        assertThat(lec.discount()).isEqualByComparingTo("3000.00");
        assertThat(lec.taxableBase()).isEqualByComparingTo("15000.00");
        assertThat(lec.taxRate()).isEqualByComparingTo("0.0000");
        assertThat(lec.tax()).isEqualByComparingTo("0.00");

        assertThat(breakdown.totalDiscounts()).isEqualByComparingTo("7000.00");
        assertThat(breakdown.totalTaxableBase()).isEqualByComparingTo("35000.00");
        assertThat(breakdown.total()).isEqualByComparingTo("37400.00");

        assertThat(breakdown.discounts()).singleElement().satisfies(discount -> {
            assertThat(discount.origin()).isEqualTo(DiscountOrigin.COMBO);
            assertThat(discount.sourceId()).isEqualTo(comboId);
            assertThat(discount.detail()).isEqualTo("Gaseosa + Leche");
            assertThat(discount.amount()).isEqualByComparingTo("7000.00");
        });
    }

    @Test
    void chargesUnitsAboveTheMinimumAtTheRegularPrice() {
        clearCombos(gtRulesetId());
        insertCombo(gtRulesetId(), "Gaseosa + Leche", "35000", 20, map("GAS-500", 6, "LEC-1L", 3));

        PriceBreakdown breakdown = service.price(guatemalaCart(map("GAS-500", 8, "LEC-1L", 3)));

        LineBreakdown gas = line(breakdown, product(gtTenantId(), "GAS-500"));

        assertThat(gas.gross()).isEqualByComparingTo("32000.00");
        assertThat(gas.discount()).isEqualByComparingTo("4000.00");
        assertThat(gas.taxableBase()).isEqualByComparingTo("28000.00");
        assertThat(gas.tax()).isEqualByComparingTo("3360.00");

        assertThat(breakdown.totalDiscounts()).isEqualByComparingTo("7000.00");
        assertThat(breakdown.totalTaxableBase()).isEqualByComparingTo("43000.00");
        assertThat(breakdown.total()).isEqualByComparingTo("46360.00");
    }

    @Test
    void doesNotApplyWhenAnItemIsBelowItsMinimum() {
        clearCombos(gtRulesetId());
        insertCombo(gtRulesetId(), "Gaseosa + Leche", "35000", 20, map("GAS-500", 6, "LEC-1L", 3));

        PriceBreakdown breakdown = service.price(guatemalaCart(map("GAS-500", 6, "LEC-1L", 2)));

        assertThat(breakdown.discounts()).isEmpty();
        assertThat(breakdown.totalDiscounts()).isEqualByComparingTo("0.00");
        assertThat(breakdown.grossSubtotal()).isEqualByComparingTo("36000.00");
        assertThat(breakdown.totalTax()).isEqualByComparingTo("2880.00");
        assertThat(breakdown.total()).isEqualByComparingTo("38880.00");
    }

    @Test
    void recordsNoSavingWhenTheComboPriceEqualsTheListValue() {
        clearCombos(gtRulesetId());
        insertCombo(gtRulesetId(), "Gaseosa + Agua", "39000", 20, map("GAS-500", 6, "AGU-500", 6));

        PriceBreakdown breakdown = service.price(guatemalaCart(map("GAS-500", 6, "AGU-500", 6)));

        assertThat(breakdown.discounts()).singleElement()
                .satisfies(discount -> assertThat(discount.amount()).isEqualByComparingTo("0.00"));
        assertThat(breakdown.totalDiscounts()).isEqualByComparingTo("0.00");
        assertThat(breakdown.totalTaxableBase()).isEqualByComparingTo("39000.00");
        assertThat(breakdown.total()).isEqualByComparingTo("43680.00");
    }

    @Test
    void proratesAcrossThreeItemsAndPutsTheRoundingRemainderInTheLastLine() {
        clearCombos(gtRulesetId());
        insertCombo(gtRulesetId(), "Surtido", "5001", 20, map("GAS-500", 1, "AGU-500", 1, "LEC-1L", 1));

        PriceBreakdown breakdown = service.price(guatemalaCart(map("GAS-500", 1, "AGU-500", 1, "LEC-1L", 1)));

        assertThat(line(breakdown, product(gtTenantId(), "GAS-500")).discount()).isEqualByComparingTo("2399.68");
        assertThat(line(breakdown, product(gtTenantId(), "AGU-500")).discount()).isEqualByComparingTo("1499.80");
        assertThat(line(breakdown, product(gtTenantId(), "LEC-1L")).discount()).isEqualByComparingTo("3599.52");

        assertThat(breakdown.totalDiscounts()).isEqualByComparingTo("7499.00");
        assertThat(breakdown.totalTaxableBase()).isEqualByComparingTo("5001.00");
        assertThat(breakdown.total()).isEqualByComparingTo("5313.06");
    }

    @Test
    void appliesCombosInPriorityOrderAndConsumesTheCoveredUnits() {
        clearCombos(gtRulesetId());
        insertCombo(gtRulesetId(), "Gaseosa + Leche", "30000", 20, map("GAS-500", 6, "LEC-1L", 3));
        insertCombo(gtRulesetId(), "Gaseosa + Leche chico", "25000", 10, map("GAS-500", 4, "LEC-1L", 2));

        PriceBreakdown breakdown = service.price(guatemalaCart(map("GAS-500", 6, "LEC-1L", 3)));

        assertThat(breakdown.discounts()).singleElement()
                .satisfies(discount -> assertThat(discount.detail()).isEqualTo("Gaseosa + Leche"));
        assertThat(breakdown.totalDiscounts()).isEqualByComparingTo("12000.00");
        assertThat(breakdown.totalTaxableBase()).isEqualByComparingTo("30000.00");
    }

    @Test
    void followsPriorityRatherThanTheBetterSaving() {
        clearCombos(gtRulesetId());
        insertCombo(gtRulesetId(), "Gaseosa + Leche", "10000", 20, map("GAS-500", 6, "LEC-1L", 6));
        long preferred = insertCombo(gtRulesetId(), "Gaseosa + Agua", "30000", 30, map("GAS-500", 6, "AGU-500", 6));

        PriceBreakdown breakdown = service.price(guatemalaCart(map("GAS-500", 6, "AGU-500", 6, "LEC-1L", 6)));

        assertThat(breakdown.discounts()).singleElement()
                .satisfies(discount -> assertThat(discount.sourceId()).isEqualTo(preferred));
        assertThat(breakdown.totalDiscounts()).isEqualByComparingTo("9000.00");
        assertThat(breakdown.totalTax()).isEqualByComparingTo("3600.00");
        assertThat(breakdown.total()).isEqualByComparingTo("69600.00");
    }

    @Test
    void doesNotApplyAnotherTenantsCombo() {
        // The GT combo (Gaseosa + Agua) is not part of the CO ruleset.
        PriceBreakdown breakdown = service.price(colombiaCart(map("GAS-400", 6, "AGU-600", 6)));

        assertThat(breakdown.currency()).isEqualTo("COP");
        assertThat(breakdown.discounts()).isEmpty();
        assertThat(breakdown.totalDiscounts()).isEqualByComparingTo("0");
    }

    @Test
    void usesTheTenantsCurrencyScale() {
        clearCombos(coRulesetId());
        insertCombo(coRulesetId(), "Gaseosa + Agua CO", "20000", 20, map("GAS-400", 6, "AGU-600", 6));

        PriceBreakdown breakdown = service.price(colombiaCart(map("GAS-400", 6, "AGU-600", 6)));

        assertThat(breakdown.currency()).isEqualTo("COP");
        assertThat(breakdown.totalDiscounts()).isEqualByComparingTo("5800");
        assertThat(breakdown.totalDiscounts().scale()).isZero();
        assertThat(breakdown.totalTaxableBase()).isEqualByComparingTo("20000");
        assertThat(breakdown.total()).isEqualByComparingTo("23800");
    }

    @Test
    void reportsNoGiftEntriesBecauseGiftsAreOutOfScope() {
        PriceBreakdown breakdown = service.price(guatemalaCart(map("GAS-500", 12)));

        assertThat(breakdown.gifts()).isEmpty();
        assertThat(breakdown.discounts()).isEmpty();
        assertThat(breakdown.total()).isEqualByComparingTo("53760.00");
    }

    private long product(long tenantId, String sku) {
        return jdbc.queryForObject("SELECT id FROM product WHERE tenant_id = ? AND sku = ?", Long.class, tenantId, sku);
    }

    private Cart guatemalaCart(Map<String, Integer> skus) {
        List<CartLine> lines = new ArrayList<>();
        skus.forEach((sku, qty) -> lines.add(new CartLine(product(gtTenantId(), sku), qty)));
        return new Cart(gtTenantId(), gtCustomerId(), paymentMethodId("CASH"), List.copyOf(lines));
    }

    private Cart colombiaCart(Map<String, Integer> skus) {
        List<CartLine> lines = new ArrayList<>();
        skus.forEach((sku, qty) -> lines.add(new CartLine(product(coTenantId(), sku), qty)));
        return new Cart(coTenantId(), coCustomerId(), paymentMethodId(coTenantId(), "CASH"), List.copyOf(lines));
    }

    private LineBreakdown line(PriceBreakdown breakdown, long productId) {
        return breakdown.lines().stream()
                .filter(line -> line.productId() == productId)
                .findFirst().orElseThrow();
    }

    private long insertCombo(long rulesetId, String name, String comboPrice, int priority, Map<String, Integer> items) {
        long id = insert("INSERT INTO combo (ruleset_id, name, combo_price, priority) VALUES (?, ?, ?, ?)",
                rulesetId, name, new BigDecimal(comboPrice), priority);
        for (Map.Entry<String, Integer> item : items.entrySet()) {
            long tenantId = rulesetId == coRulesetId() ? coTenantId() : gtTenantId();
            insert("INSERT INTO combo_item (combo_id, product_id, min_qty) VALUES (?, ?, ?)",
                    id, product(tenantId, item.getKey()), item.getValue());
        }
        return id;
    }

    private void clearCombos(long rulesetId) {
        jdbc.update("DELETE FROM combo_item WHERE combo_id IN (SELECT id FROM combo WHERE ruleset_id = ?)", rulesetId);
        jdbc.update("DELETE FROM combo WHERE ruleset_id = ?", rulesetId);
    }

    private static Map<String, Integer> map(Object... pairs) {
        Map<String, Integer> entries = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            entries.put((String) pairs[i], (Integer) pairs[i + 1]);
        }
        return entries;
    }
}
