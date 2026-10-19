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

class GiftPricingTest extends AbstractIntegrationTest {

    @Autowired
    private OrderPricingService service;

    @BeforeEach
    void clearLevers() {
        jdbc.update("DELETE FROM gift WHERE ruleset_id = ?", gtRulesetId());
        jdbc.update("DELETE FROM gift WHERE ruleset_id = ?", coRulesetId());
        jdbc.update("DELETE FROM combo_item WHERE combo_id IN (SELECT id FROM combo WHERE ruleset_id = ?)",
                gtRulesetId());
        jdbc.update("DELETE FROM combo WHERE ruleset_id = ?", gtRulesetId());
        jdbc.update("DELETE FROM scale_tier WHERE scale_discount_id IN "
                + "(SELECT id FROM scale_discount WHERE ruleset_id = ?)", gtRulesetId());
        jdbc.update("DELETE FROM scale_discount WHERE ruleset_id = ?", gtRulesetId());
    }

    @Test
    void appliesAQuantityCondition() {
        give(gtRulesetId(), condition(gtProduct("GAS-500"), null, 12, null), "AGU-500", 1, false);

        PriceBreakdown breakdown = service.price(guatemalaCart(map("GAS-500", 12)));

        LineBreakdown gift = giftLine(breakdown, gtProduct("AGU-500"));
        assertThat(gift.qty()).isEqualTo(1);
        assertThat(gift.gross()).isEqualByComparingTo("2500.00");
        assertThat(gift.discount()).isEqualByComparingTo("2500.00");
        assertThat(gift.taxableBase()).isEqualByComparingTo("0.00");
        assertThat(gift.tax()).isEqualByComparingTo("0.00");

        assertThat(breakdown.discounts()).singleElement().satisfies(discount -> {
            assertThat(discount.origin()).isEqualTo(DiscountOrigin.GIFT);
            assertThat(discount.amount()).isEqualByComparingTo("2500.00");
        });
        assertThat(breakdown.grossSubtotal()).isEqualByComparingTo("50500.00");
        assertThat(breakdown.totalDiscounts()).isEqualByComparingTo("2500.00");
        assertThat(breakdown.totalTaxableBase()).isEqualByComparingTo("48000.00");
        assertThat(breakdown.totalTax()).isEqualByComparingTo("5760.00");
        assertThat(breakdown.total()).isEqualByComparingTo("53760.00");
    }

    @Test
    void appliesAnAmountCondition() {
        give(gtRulesetId(), condition(null, null, null, "50000"), "AGU-500", 1, false);

        PriceBreakdown breakdown = service.price(guatemalaCart(map("LEC-1L", 10)));

        assertThat(giftLine(breakdown, gtProduct("AGU-500")).qty()).isEqualTo(1);
        assertThat(breakdown.totalDiscounts()).isEqualByComparingTo("2500.00");
        assertThat(breakdown.totalTaxableBase()).isEqualByComparingTo("60000.00");
        assertThat(breakdown.total()).isEqualByComparingTo("60000.00");
    }

    @Test
    void requiresBothThresholds() {
        give(gtRulesetId(), condition(gtProduct("GAS-500"), null, 12, "60000"), "AGU-500", 1, false);

        PriceBreakdown belowAmount = service.price(guatemalaCart(map("GAS-500", 12)));
        assertThat(belowAmount.discounts()).isEmpty();
        assertThat(belowAmount.lines()).hasSize(1);

        PriceBreakdown met = service.price(guatemalaCart(map("GAS-500", 16)));
        assertThat(giftLine(met, gtProduct("AGU-500")).qty()).isEqualTo(1);
        assertThat(met.total()).isEqualByComparingTo("71680.00");
    }

    @Test
    void taxesATaxableGiftOnItsValue() {
        give(gtRulesetId(), condition(gtProduct("GAS-500"), null, 12, null), "AGU-500", 1, true);

        PriceBreakdown breakdown = service.price(guatemalaCart(map("GAS-500", 12)));

        LineBreakdown gift = giftLine(breakdown, gtProduct("AGU-500"));
        assertThat(gift.taxableBase()).isEqualByComparingTo("0.00");
        assertThat(gift.tax()).isEqualByComparingTo("300.00");
        assertThat(breakdown.totalTax()).isEqualByComparingTo("6060.00");
        assertThat(breakdown.total()).isEqualByComparingTo("54060.00");
    }

    @Test
    void accumulatesEveryMetCondition() {
        give(gtRulesetId(), condition(gtProduct("GAS-500"), null, 12, null), "AGU-500", 1, false);
        give(gtRulesetId(), condition(null, null, null, "50000"), "AGU-500", 1, false);

        PriceBreakdown breakdown = service.price(guatemalaCart(map("GAS-500", 13)));

        assertThat(breakdown.lines()).filteredOn(LineBreakdown::gift).hasSize(2);
        assertThat(breakdown.discounts()).extracting(DiscountBreakdown::origin)
                .containsOnly(DiscountOrigin.GIFT).hasSize(2);
        assertThat(breakdown.totalDiscounts()).isEqualByComparingTo("5000.00");
        assertThat(breakdown.total()).isEqualByComparingTo("58240.00");
    }

    @Test
    void giftLineDoesNotTriggerAScale() {
        insertScale(DiscountTargetType.FAMILY, null, familyId("Aguas"), 10, 5);
        give(gtRulesetId(), condition(null, null, null, "0"), "AGU-500", 10, false);

        PriceBreakdown breakdown = service.price(guatemalaCart(map("AGU-500", 1)));

        assertThat(breakdown.discounts()).extracting(DiscountBreakdown::origin)
                .containsOnly(DiscountOrigin.GIFT);
        assertThat(paidLine(breakdown, gtProduct("AGU-500")).qty()).isEqualTo(1);
        assertThat(giftLine(breakdown, gtProduct("AGU-500")).qty()).isEqualTo(10);
        assertThat(breakdown.total()).isEqualByComparingTo("2800.00");
    }

    @Test
    void keepsASeparateGiftLineWhenTheCartHasTheProduct() {
        give(gtRulesetId(), condition(gtProduct("GAS-500"), null, 12, null), "AGU-500", 1, false);

        PriceBreakdown breakdown = service.price(guatemalaCart(map("GAS-500", 12, "AGU-500", 1)));

        assertThat(breakdown.lines()).hasSize(3);
        assertThat(paidLine(breakdown, gtProduct("AGU-500")).gift()).isFalse();
        assertThat(paidLine(breakdown, gtProduct("AGU-500")).tax()).isEqualByComparingTo("300.00");
        assertThat(giftLine(breakdown, gtProduct("AGU-500")).tax()).isEqualByComparingTo("0.00");
        assertThat(breakdown.total()).isEqualByComparingTo("56560.00");
    }

    @Test
    void doesNotApplyAnotherTenantsGift() {
        give(gtRulesetId(), condition(gtProduct("GAS-500"), null, 12, null), "AGU-500", 1, false);

        PriceBreakdown breakdown = service.price(colombiaCart(map("GAS-400", 6)));

        assertThat(breakdown.discounts()).isEmpty();
        assertThat(breakdown.lines()).hasSize(1);
        assertThat(breakdown.total()).isEqualByComparingTo("17850");
    }

    @Test
    void usesTheTenantsCurrencyScale() {
        give(coRulesetId(), condition(coProduct("GAS-400"), null, 6, null), "AGU-600", 1, true);

        PriceBreakdown breakdown = service.price(colombiaCart(map("GAS-400", 6)));

        LineBreakdown gift = giftLine(breakdown, coProduct("AGU-600"));
        assertThat(gift.taxableBase()).isEqualByComparingTo("0");
        assertThat(gift.tax()).isEqualByComparingTo("342");
        assertThat(gift.tax().scale()).isZero();
        assertThat(breakdown.totalTax()).isEqualByComparingTo("3192");
        assertThat(breakdown.total()).isEqualByComparingTo("18192");
    }

    private long condition(Long productId, Long familyId, Integer minQty, String minAmount) {
        return insert("INSERT INTO gift_condition (product_id, family_id, min_qty, min_amount) VALUES (?, ?, ?, ?)",
                productId, familyId, minQty, minAmount == null ? null : new BigDecimal(minAmount));
    }

    private void give(long rulesetId, long conditionId, String giftSku, int qty, boolean taxable) {
        long tenantId = rulesetId == coRulesetId() ? coTenantId() : gtTenantId();
        insert("INSERT INTO gift (ruleset_id, condition_id, gift_product_id, gift_qty, taxable) "
                + "VALUES (?, ?, ?, ?, ?)", rulesetId, conditionId, productId(tenantId, giftSku), qty, taxable);
    }

    private void insertScale(DiscountTargetType type, Long productId, Long familyId, int fromQty, int percentage) {
        long id = insert("INSERT INTO scale_discount (ruleset_id, target_type, product_id, family_id, priority) "
                + "VALUES (?, ?, ?, ?, 0)", gtRulesetId(), type.name(), productId, familyId);
        insert("INSERT INTO scale_tier (scale_discount_id, from_qty, percentage) VALUES (?, ?, ?)",
                id, fromQty, new BigDecimal(percentage));
    }

    private long gtProduct(String sku) {
        return productId(gtTenantId(), sku);
    }

    private long coProduct(String sku) {
        return productId(coTenantId(), sku);
    }

    private long productId(long tenantId, String sku) {
        return jdbc.queryForObject("SELECT id FROM product WHERE tenant_id = ? AND sku = ?", Long.class, tenantId, sku);
    }

    private long familyId(String name) {
        return jdbc.queryForObject("SELECT id FROM product_family WHERE tenant_id = ? AND name = ?", Long.class,
                gtTenantId(), name);
    }

    private Cart guatemalaCart(Map<String, Integer> skus) {
        List<CartLine> lines = new ArrayList<>();
        skus.forEach((sku, qty) -> lines.add(new CartLine(gtProduct(sku), qty)));
        return new Cart(gtTenantId(), gtCustomerId(), paymentMethodId("CASH"), List.copyOf(lines));
    }

    private Cart colombiaCart(Map<String, Integer> skus) {
        List<CartLine> lines = new ArrayList<>();
        skus.forEach((sku, qty) -> lines.add(new CartLine(coProduct(sku), qty)));
        return new Cart(coTenantId(), coCustomerId(), paymentMethodId(coTenantId(), "CASH"), List.copyOf(lines));
    }

    private LineBreakdown giftLine(PriceBreakdown breakdown, long productId) {
        return breakdown.lines().stream()
                .filter(line -> line.productId() == productId && line.gift())
                .findFirst().orElseThrow();
    }

    private LineBreakdown paidLine(PriceBreakdown breakdown, long productId) {
        return breakdown.lines().stream()
                .filter(line -> line.productId() == productId && !line.gift())
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
