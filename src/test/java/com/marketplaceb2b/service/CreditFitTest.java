package com.marketplaceb2b.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.marketplaceb2b.dto.CreditFit;
import com.marketplaceb2b.dto.CreditFitReason;
import com.marketplaceb2b.dto.PriceBreakdown;
import com.marketplaceb2b.model.DiscountTargetType;
import com.marketplaceb2b.service.exception.MissingPaymentMethodException;
import com.marketplaceb2b.support.AbstractIntegrationTest;

class CreditFitTest extends AbstractIntegrationTest {

    @Autowired
    private OrderPricingService service;

    @BeforeEach
    void clearLevers() {
        jdbc.update("DELETE FROM gift WHERE ruleset_id = ?", gtRulesetId());
        jdbc.update("DELETE FROM combo_item WHERE combo_id IN (SELECT id FROM combo WHERE ruleset_id = ?)",
                gtRulesetId());
        jdbc.update("DELETE FROM combo WHERE ruleset_id = ?", gtRulesetId());
        jdbc.update("DELETE FROM scale_tier WHERE scale_discount_id IN "
                + "(SELECT id FROM scale_discount WHERE ruleset_id = ?)", gtRulesetId());
        jdbc.update("DELETE FROM scale_discount WHERE ruleset_id = ?", gtRulesetId());
    }

    @Test
    void fitsWithACombo() {
        insertCombo("Gaseosa + Agua", "20000", 20, "GAS-500", 6, "AGU-500", 6);

        PriceBreakdown breakdown = service.price(cart(gtCustomerId(), credit(), map(
                "LEC-1L", 46, "GAS-500", 6, "AGU-500", 6)));

        assertThat(breakdown.total()).isEqualByComparingTo("298400.00");
        assertFit(breakdown, CreditFitReason.FITS, "300000.00", "298400.00");
    }

    @Test
    void fitsExactlyAtTheAvailable() {
        PriceBreakdown breakdown = service.price(cart(gtCustomerId(), credit(), map("LEC-1L", 50)));

        assertFit(breakdown, CreditFitReason.FITS, "300000.00", "300000.00");
    }

    @Test
    void insufficientAboveTheAvailable() {
        PriceBreakdown breakdown = service.price(cart(gtCustomerId(), credit(), map("LEC-1L", 51)));

        assertFit(breakdown, CreditFitReason.INSUFFICIENT_CREDIT, "300000.00", "306000.00");
    }

    @Test
    void insufficientWhenTheTaxPushesItOverWithAScale() {
        insertScale(DiscountTargetType.SKU, gtProduct("GAS-500"), null, 10, 24, 7);

        PriceBreakdown breakdown = service.price(cart(gtCustomerId(), credit(), map("GAS-500", 80)));

        assertThat(breakdown.totalTaxableBase()).isEqualByComparingTo("297600.00");
        assertFit(breakdown, CreditFitReason.INSUFFICIENT_CREDIT, "300000.00", "333312.00");
    }

    @Test
    void insufficientWithAnEmptyAccount() {
        setAvailable("0");

        PriceBreakdown breakdown = service.price(cart(gtCustomerId(), credit(), map("LEC-1L", 1)));

        assertFit(breakdown, CreditFitReason.INSUFFICIENT_CREDIT, "0.00", "6000.00");
    }

    @Test
    void noAccountForACreditMethodWithoutAnAccount() {
        give(condition(null, null, null, "0"), "AGU-500", 1, false);
        long customer = customerWithoutAccount();

        PriceBreakdown breakdown = service.price(cart(customer, credit(), map("LEC-1L", 5)));

        assertFit(breakdown, CreditFitReason.NO_ACCOUNT, "0.00", "30000.00");
    }

    @Test
    void noAccountWithoutACustomer() {
        PriceBreakdown breakdown = service.price(cart(null, credit(), map("LEC-1L", 5)));

        assertFit(breakdown, CreditFitReason.NO_ACCOUNT, "0.00", "30000.00");
    }

    @Test
    void nullForANonCreditMethod() {
        insertCombo("Gaseosa + Agua", "20000", 20, "GAS-500", 6, "AGU-500", 6);

        PriceBreakdown breakdown = service.price(cart(gtCustomerId(), paymentMethodId("CASH"),
                map("GAS-500", 6, "AGU-500", 6)));

        assertThat(breakdown.creditFit()).isNull();
    }

    @Test
    void nullWithoutAPaymentMethod() {
        PriceBreakdown breakdown = service.price(cart(gtCustomerId(), null, map("LEC-1L", 5)));

        assertThat(breakdown.creditFit()).isNull();
    }

    @Test
    void rejectsAnUnknownPaymentMethod() {
        Cart cart = cart(gtCustomerId(), 999999L, map("LEC-1L", 1));

        assertThatThrownBy(() -> service.price(cart)).isInstanceOf(MissingPaymentMethodException.class);
    }

    @Test
    void rejectsAnotherTenantsPaymentMethod() {
        Cart cart = cart(gtCustomerId(), paymentMethodId(coTenantId(), "CREDIT"), map("LEC-1L", 1));

        assertThatThrownBy(() -> service.price(cart)).isInstanceOf(MissingPaymentMethodException.class);
    }

    @Test
    void noAccountForACustomerFromAnotherTenant() {
        Cart cart = cart(coCustomerId(), credit(), map("LEC-1L", 5));

        PriceBreakdown breakdown = service.price(cart);

        assertFit(breakdown, CreditFitReason.NO_ACCOUNT, "0.00", "30000.00");
    }

    @Test
    void allThreeLeversFit() {
        setAvailable("135000");
        insertCombo("Gaseosa + Agua", "20000", 20, "GAS-500", 6, "AGU-500", 6);
        insertScale(DiscountTargetType.SKU, gtProduct("GAS-500"), null, 10, 12, 3);
        give(condition(null, null, null, "0"), "AGU-500", 10, false);

        PriceBreakdown breakdown = service.price(cart(gtCustomerId(), credit(), map(
                "GAS-500", 18, "AGU-500", 6, "LEC-1L", 10)));

        assertThat(breakdown.total()).isEqualByComparingTo("134547.20");
        assertFit(breakdown, CreditFitReason.FITS, "135000.00", "134547.20");
    }

    @Test
    void aTaxableGiftBreaksTheFit() {
        setAvailable("135000");
        insertCombo("Gaseosa + Agua", "20000", 20, "GAS-500", 6, "AGU-500", 6);
        insertScale(DiscountTargetType.SKU, gtProduct("GAS-500"), null, 10, 12, 3);
        give(condition(null, null, null, "0"), "AGU-500", 10, true);

        PriceBreakdown breakdown = service.price(cart(gtCustomerId(), credit(), map(
                "GAS-500", 18, "AGU-500", 6, "LEC-1L", 10)));

        assertThat(breakdown.total()).isEqualByComparingTo("137547.20");
        assertFit(breakdown, CreditFitReason.INSUFFICIENT_CREDIT, "135000.00", "137547.20");
    }

    private void assertFit(PriceBreakdown breakdown, CreditFitReason reason, String available, String total) {
        CreditFit fit = breakdown.creditFit();
        assertThat(fit).isNotNull();
        assertThat(fit.reason()).isEqualTo(reason);
        assertThat(fit.fits()).isEqualTo(reason == CreditFitReason.FITS);
        assertThat(fit.available()).isEqualByComparingTo(available);
        assertThat(fit.total()).isEqualByComparingTo(total);
    }

    private long credit() {
        return paymentMethodId("CREDIT");
    }

    private Cart cart(Long customerId, Long paymentMethodId, Map<String, Integer> skus) {
        List<CartLine> lines = new ArrayList<>();
        skus.forEach((sku, qty) -> lines.add(new CartLine(gtProduct(sku), qty)));
        return new Cart(gtTenantId(), customerId, paymentMethodId, List.copyOf(lines));
    }

    private long gtProduct(String sku) {
        return jdbc.queryForObject("SELECT id FROM product WHERE tenant_id = ? AND sku = ?", Long.class, gtTenantId(),
                sku);
    }

    private long customerWithoutAccount() {
        return insert("INSERT INTO customer (tenant_id, name, fiscal_id, fiscal_id_type_id) VALUES (?, ?, ?, "
                        + "(SELECT id FROM fiscal_id_type WHERE code = 'NIT' "
                        + "AND country_id = (SELECT id FROM country WHERE code = 'GT')))",
                gtTenantId(), unique("Cliente"), "12345678");
    }

    private void setAvailable(String amount) {
        jdbc.update("UPDATE credit_account SET available = ? WHERE customer_id = ?", new BigDecimal(amount),
                gtCustomerId());
    }

    private long condition(Long productId, Long familyId, Integer minQty, String minAmount) {
        return insert("INSERT INTO gift_condition (product_id, family_id, min_qty, min_amount) VALUES (?, ?, ?, ?)",
                productId, familyId, minQty, minAmount == null ? null : new BigDecimal(minAmount));
    }

    private void give(long conditionId, String giftSku, int qty, boolean taxable) {
        insert("INSERT INTO gift (ruleset_id, condition_id, gift_product_id, gift_qty, taxable) "
                + "VALUES (?, ?, ?, ?, ?)", gtRulesetId(), conditionId, gtProduct(giftSku), qty, taxable);
    }

    private void insertCombo(String name, String comboPrice, int priority, Object... productQty) {
        long id = insert("INSERT INTO combo (ruleset_id, name, combo_price, priority) VALUES (?, ?, ?, ?)",
                gtRulesetId(), name, new BigDecimal(comboPrice), priority);
        for (int i = 0; i < productQty.length; i += 2) {
            insert("INSERT INTO combo_item (combo_id, product_id, min_qty) VALUES (?, ?, ?)",
                    id, gtProduct((String) productQty[i]), productQty[i + 1]);
        }
    }

    private void insertScale(DiscountTargetType type, Long productId, Long familyId, int priority, int fromQty,
                             int percentage) {
        long id = insert("INSERT INTO scale_discount (ruleset_id, target_type, product_id, family_id, priority) "
                + "VALUES (?, ?, ?, ?, ?)", gtRulesetId(), type.name(), productId, familyId, priority);
        insert("INSERT INTO scale_tier (scale_discount_id, from_qty, percentage) VALUES (?, ?, ?)",
                id, fromQty, new BigDecimal(percentage));
    }

    private static Map<String, Integer> map(Object... pairs) {
        Map<String, Integer> entries = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            entries.put((String) pairs[i], (Integer) pairs[i + 1]);
        }
        return entries;
    }
}
