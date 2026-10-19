package com.marketplaceb2b.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.marketplaceb2b.dto.Cart;
import com.marketplaceb2b.dto.CartLine;
import com.marketplaceb2b.dto.DiscountBreakdown;
import com.marketplaceb2b.dto.DiscountOrigin;
import com.marketplaceb2b.dto.LineBreakdown;
import com.marketplaceb2b.dto.PriceBreakdown;
import com.marketplaceb2b.model.DiscountTargetType;
import com.marketplaceb2b.support.AbstractIntegrationTest;

class ScalePricingTest extends AbstractIntegrationTest {

    @Autowired
    private OrderPricingService service;

    @BeforeEach
    void clearGifts() {
        jdbc.update("DELETE FROM gift WHERE ruleset_id = ?", gtRulesetId());
    }

    @Test
    void appliesTheSkuScaleAtItsFirstTier() {
        PriceBreakdown breakdown = service.price(guatemalaCart(map("GAS-500", 12)));

        assertThat(line(breakdown, productId(gtTenantId(), "GAS-500")).discount())
                .isEqualByComparingTo("1440.00");
        assertThat(breakdown.totalDiscounts()).isEqualByComparingTo("1440.00");
        assertThat(breakdown.totalTaxableBase()).isEqualByComparingTo("46560.00");
        assertThat(breakdown.totalTax()).isEqualByComparingTo("5587.20");
        assertThat(breakdown.total()).isEqualByComparingTo("52147.20");
        assertThat(breakdown.discounts()).singleElement().satisfies(discount -> {
            assertThat(discount.origin()).isEqualTo(DiscountOrigin.SCALE);
            assertThat(discount.sourceId()).isEqualTo(skuScaleId("GAS-500"));
        });
    }

    @Test
    void appliesTheReachedHigherTierToAllUnits() {
        PriceBreakdown breakdown = service.price(guatemalaCart(map("GAS-500", 30)));

        assertThat(line(breakdown, productId(gtTenantId(), "GAS-500")).discount())
                .isEqualByComparingTo("8400.00");
        assertThat(breakdown.totalDiscounts()).isEqualByComparingTo("8400.00");
        assertThat(breakdown.totalTaxableBase()).isEqualByComparingTo("111600.00");
        assertThat(breakdown.total()).isEqualByComparingTo("124992.00");
        assertThat(breakdown.discounts()).singleElement()
                .satisfies(discount -> assertThat(discount.sourceId()).isEqualTo(skuScaleId("GAS-500")));
    }

    @Test
    void appliesTheFamilyScaleWhenTheSkuDoesNotReachItsTier() {
        PriceBreakdown breakdown = service.price(guatemalaCart(map("GAS-500", 10)));

        assertThat(line(breakdown, productId(gtTenantId(), "GAS-500")).discount())
                .isEqualByComparingTo("2000.00");
        assertThat(breakdown.totalTaxableBase()).isEqualByComparingTo("38000.00");
        assertThat(breakdown.total()).isEqualByComparingTo("42560.00");
        assertThat(breakdown.discounts()).singleElement()
                .satisfies(discount -> assertThat(discount.sourceId()).isEqualTo(familyScaleId("Gaseosas")));
    }

    @Test
    void appliesTwoScalesAtTwoTiersInOneOrder() {
        long lecScale = insertScale(gtRulesetId(), DiscountTargetType.SKU, productId(gtTenantId(), "LEC-1L"),
                null, 10, 10, 5);

        PriceBreakdown breakdown = service.price(guatemalaCart(map("GAS-500", 24, "LEC-1L", 10)));

        assertThat(breakdown.discounts()).extracting(DiscountBreakdown::sourceId)
                .containsExactlyInAnyOrder(skuScaleId("GAS-500"), lecScale);
        assertThat(line(breakdown, productId(gtTenantId(), "GAS-500")).discount())
                .isEqualByComparingTo("6720.00");
        assertThat(line(breakdown, productId(gtTenantId(), "LEC-1L")).discount())
                .isEqualByComparingTo("3000.00");
        assertThat(breakdown.totalDiscounts()).isEqualByComparingTo("9720.00");
        assertThat(breakdown.totalTax()).isEqualByComparingTo("10713.60");
        assertThat(breakdown.total()).isEqualByComparingTo("156993.60");
    }

    @Test
    void doesNotApplyBelowTheFirstTier() {
        PriceBreakdown breakdown = service.price(guatemalaCart(map("GAS-500", 5)));

        assertThat(breakdown.discounts()).isEmpty();
        assertThat(breakdown.totalDiscounts()).isEqualByComparingTo("0.00");
        assertThat(breakdown.total()).isEqualByComparingTo("22400.00");
    }

    @Test
    void priorityBeatsSpecificity() {
        long familyHigherPriority = insertScale(gtRulesetId(), DiscountTargetType.FAMILY, null,
                familyId("Gaseosas"), 15, 10, 5);

        PriceBreakdown breakdown = service.price(guatemalaCart(map("GAS-500", 12)));

        assertThat(breakdown.discounts()).singleElement().satisfies(discount -> {
            assertThat(discount.sourceId()).isEqualTo(familyHigherPriority);
            assertThat(discount.amount()).isEqualByComparingTo("2400.00");
        });
        assertThat(breakdown.total()).isEqualByComparingTo("51072.00");
    }

    @Test
    void theComboConsumesItsUnitsBeforeTheScale() {
        PriceBreakdown breakdown = service.price(guatemalaCart(map("GAS-500", 18, "AGU-500", 6)));

        assertThat(breakdown.discounts()).extracting(DiscountBreakdown::origin)
                .containsExactly(DiscountOrigin.COMBO, DiscountOrigin.SCALE);
        assertThat(line(breakdown, productId(gtTenantId(), "GAS-500")).discount())
                .isEqualByComparingTo("13132.31");
        assertThat(line(breakdown, productId(gtTenantId(), "AGU-500")).discount())
                .isEqualByComparingTo("7307.69");
        assertThat(breakdown.totalDiscounts()).isEqualByComparingTo("20440.00");
        assertThat(breakdown.total()).isEqualByComparingTo("74547.20");
    }

    @Test
    void usesTheTenantsCurrencyScale() {
        insertScale(coRulesetId(), DiscountTargetType.SKU, productId(coTenantId(), "GAS-400"), null, 10, 6, 10);

        PriceBreakdown breakdown = service.price(colombiaCart(map("GAS-400", 6)));

        assertThat(breakdown.currency()).isEqualTo("COP");
        assertThat(breakdown.totalDiscounts()).isEqualByComparingTo("1500");
        assertThat(breakdown.totalDiscounts().scale()).isZero();
        assertThat(breakdown.totalTaxableBase()).isEqualByComparingTo("13500");
        assertThat(breakdown.total()).isEqualByComparingTo("16065");
    }

    @Test
    void doesNotApplyAnotherTenantsScale() {
        PriceBreakdown breakdown = service.price(colombiaCart(map("GAS-400", 12)));

        assertThat(breakdown.currency()).isEqualTo("COP");
        assertThat(breakdown.discounts()).isEmpty();
        assertThat(breakdown.total()).isEqualByComparingTo("35700");
    }

    @Test
    void appliesScalesOnProductsWithDifferentTaxRates() {
        insertScale(gtRulesetId(), DiscountTargetType.SKU, productId(gtTenantId(), "LEC-1L"), null, 10, 4, 10);

        PriceBreakdown breakdown = service.price(guatemalaCart(map("GAS-500", 12, "LEC-1L", 4)));

        assertThat(breakdown.discounts()).extracting(DiscountBreakdown::origin)
                .containsOnly(DiscountOrigin.SCALE);
        assertThat(breakdown.discounts()).hasSize(2);
        assertThat(line(breakdown, productId(gtTenantId(), "LEC-1L")).discount())
                .isEqualByComparingTo("2400.00");
        assertThat(line(breakdown, productId(gtTenantId(), "LEC-1L")).tax()).isEqualByComparingTo("0.00");
        assertThat(line(breakdown, productId(gtTenantId(), "GAS-500")).tax()).isEqualByComparingTo("5587.20");
        assertThat(breakdown.totalDiscounts()).isEqualByComparingTo("3840.00");
        assertThat(breakdown.total()).isEqualByComparingTo("73747.20");
    }

    private long productId(long tenantId, String sku) {
        return jdbc.queryForObject("SELECT id FROM product WHERE tenant_id = ? AND sku = ?", Long.class, tenantId, sku);
    }

    private long familyId(String name) {
        return jdbc.queryForObject("SELECT id FROM product_family WHERE tenant_id = ? AND name = ?", Long.class,
                gtTenantId(), name);
    }

    private long skuScaleId(String sku) {
        return jdbc.queryForObject("SELECT id FROM scale_discount WHERE ruleset_id = ? AND target_type = 'SKU' "
                + "AND product_id = ?", Long.class, gtRulesetId(), productId(gtTenantId(), sku));
    }

    private long familyScaleId(String familyName) {
        return jdbc.queryForObject("SELECT id FROM scale_discount WHERE ruleset_id = ? AND target_type = 'FAMILY' "
                + "AND family_id = ?", Long.class, gtRulesetId(), familyId(familyName));
    }

    private long insertScale(long rulesetId, DiscountTargetType type, Long productId, Long familyId, int priority,
                             int... fromQtyPercentage) {
        long id = insert("INSERT INTO scale_discount (ruleset_id, target_type, product_id, family_id, priority) "
                + "VALUES (?, ?, ?, ?, ?)", rulesetId, type.name(), productId, familyId, priority);
        for (int i = 0; i < fromQtyPercentage.length; i += 2) {
            insert("INSERT INTO scale_tier (scale_discount_id, from_qty, percentage) VALUES (?, ?, ?)",
                    id, fromQtyPercentage[i], new BigDecimal(fromQtyPercentage[i + 1]));
        }
        return id;
    }

    private Cart guatemalaCart(Map<String, Integer> skus) {
        List<CartLine> lines = new ArrayList<>();
        skus.forEach((sku, qty) -> lines.add(new CartLine(productId(gtTenantId(), sku), qty)));
        return new Cart(gtTenantId(), gtCustomerId(), paymentMethodId("CASH"), List.copyOf(lines));
    }

    private Cart colombiaCart(Map<String, Integer> skus) {
        List<CartLine> lines = new ArrayList<>();
        skus.forEach((sku, qty) -> lines.add(new CartLine(productId(coTenantId(), sku), qty)));
        return new Cart(coTenantId(), coCustomerId(), paymentMethodId(coTenantId(), "CASH"), List.copyOf(lines));
    }

    private LineBreakdown line(PriceBreakdown breakdown, long productId) {
        return breakdown.lines().stream()
                .filter(line -> line.productId() == productId)
                .findFirst().orElseThrow();
    }

    private static Map<String, Integer> map(Object... pairs) {
        Map<String, Integer> entries = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            entries.put((String) pairs[i], (Integer) pairs[i + 1]);
        }
        return entries;
    }
}
