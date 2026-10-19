package com.marketplaceb2b.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.marketplaceb2b.dto.Cart;
import com.marketplaceb2b.dto.CartLine;
import com.marketplaceb2b.dto.CreatedQuote;
import com.marketplaceb2b.dto.CreditFitReason;
import com.marketplaceb2b.model.DiscountTargetType;
import com.marketplaceb2b.service.exception.MissingCustomerException;
import com.marketplaceb2b.service.exception.MissingPaymentMethodException;
import com.marketplaceb2b.service.exception.MissingPriceException;
import com.marketplaceb2b.support.AbstractIntegrationTest;

class QuoteCreationTest extends AbstractIntegrationTest {

    @Autowired
    private QuoteWriter writer;

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
    void persistsAQuoteWithoutDiscounts() {
        CreatedQuote created = writer.create(cart(gtCustomerId(), cash(), map("GAS-500", 1, "LEC-1L", 2)));
        long quoteId = quoteId(created);

        assertThat(quoteTotal(quoteId)).isEqualByComparingTo("16480.00");
        assertThat(count("SELECT COUNT(*) FROM quote_line WHERE quote_id = ?", quoteId)).isEqualTo(2);
        assertThat(discountCount(quoteId)).isZero();
        assertThat(count("SELECT COUNT(*) FROM quote_gift WHERE quote_id = ?", quoteId)).isZero();
    }

    @Test
    void persistsTwoDiscountsPerLine() {
        insertCombo("Gaseosa + Agua", "20000", 20, "GAS-500", 6, "AGU-500", 6);
        insertScale(DiscountTargetType.SKU, gtProduct("GAS-500"), null, 10, 12, 3);

        CreatedQuote created = writer.create(cart(gtCustomerId(), cash(), map("GAS-500", 18, "AGU-500", 6)));
        long quoteId = quoteId(created);

        assertThat(quoteTotal(quoteId)).isEqualByComparingTo("74547.20");
        assertThat(count("SELECT COUNT(*) FROM quote_line WHERE quote_id = ?", quoteId)).isEqualTo(2);
        assertThat(discountCount(quoteId)).isEqualTo(3);
        assertThat(lineDiscountCount(quoteId, "GAS-500", "combo_id")).isEqualTo(1);
        assertThat(lineDiscountCount(quoteId, "GAS-500", "scale_discount_id")).isEqualTo(1);
        assertThat(lineDiscountCount(quoteId, "AGU-500", "combo_id")).isEqualTo(1);
        assertThat(lineDiscountCount(quoteId, "AGU-500", "scale_discount_id")).isZero();
        assertThat(amount(quoteId, "GAS-500", "combo_id")).isEqualByComparingTo("11692.31");
        assertThat(amount(quoteId, "AGU-500", "combo_id")).isEqualByComparingTo("7307.69");
        assertThat(amount(quoteId, "GAS-500", "scale_discount_id")).isEqualByComparingTo("1440.00");
    }

    @Test
    void persistsAGiftOfAProductNotInTheCart() {
        give(gtRulesetId(), condition(gtProduct("GAS-500"), null, 12, null), "AGU-500", 1, false);

        CreatedQuote created = writer.create(cart(gtCustomerId(), cash(), map("GAS-500", 12)));
        long quoteId = quoteId(created);

        assertThat(count("SELECT COUNT(*) FROM quote_line WHERE quote_id = ?", quoteId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM quote_gift WHERE quote_id = ?", quoteId)).isEqualTo(1);
        assertThat(quoteGift(quoteId, "qty", Integer.class)).isEqualTo(1);
        assertThat(quoteGift(quoteId, "taxable", Boolean.class)).isFalse();
        assertThat(quoteGift(quoteId, "gift_value", BigDecimal.class)).isEqualByComparingTo("2500.00");
        assertThat(quoteGift(quoteId, "tax", BigDecimal.class)).isEqualByComparingTo("0.00");
    }

    @Test
    void persistsAGiftOfAProductAlsoPaid() {
        give(gtRulesetId(), condition(gtProduct("GAS-500"), null, 12, null), "AGU-500", 1, false);

        CreatedQuote created = writer.create(cart(gtCustomerId(), cash(), map("GAS-500", 12, "AGU-500", 1)));
        long quoteId = quoteId(created);

        assertThat(count("SELECT COUNT(*) FROM quote_line WHERE quote_id = ?", quoteId)).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM quote_gift WHERE quote_id = ?", quoteId)).isEqualTo(1);
        assertThat(quoteGift(quoteId, "product_id", Long.class)).isEqualTo(gtProduct("AGU-500"));
    }

    @Test
    void reportsTheCreditFitAndTheExpiry() {
        CreatedQuote created = writer.create(cart(gtCustomerId(), credit(), map("LEC-1L", 5)));

        assertThat(created.breakdown().creditFit().reason()).isEqualTo(CreditFitReason.FITS);
        assertThat(created.breakdown().creditFit().available()).isEqualByComparingTo("300000.00");
        assertThat(created.expiresAt()).isBetween(Instant.now().plus(23, ChronoUnit.HOURS),
                Instant.now().plus(25, ChronoUnit.HOURS));
    }

    @Test
    void reportsInsufficientCredit() {
        setAvailable("0");

        CreatedQuote created = writer.create(cart(gtCustomerId(), credit(), map("LEC-1L", 1)));

        assertThat(created.breakdown().creditFit().reason()).isEqualTo(CreditFitReason.INSUFFICIENT_CREDIT);
    }

    @Test
    void reportsNoAccount() {
        CreatedQuote created = writer.create(cart(customerWithoutAccount(), credit(), map("LEC-1L", 1)));

        assertThat(created.breakdown().creditFit().reason()).isEqualTo(CreditFitReason.NO_ACCOUNT);
    }

    @Test
    void aLaterPriceChangeDoesNotAlterTheStoredQuote() {
        CreatedQuote created = writer.create(cart(gtCustomerId(), cash(), map("LEC-1L", 2)));
        long quoteId = quoteId(created);

        jdbc.update("UPDATE product_price SET unit_price = ? WHERE ruleset_id = ? AND product_id = ?",
                new BigDecimal("9999.0000"), gtRulesetId(), gtProduct("LEC-1L"));

        assertThat(quoteTotal(quoteId)).isEqualByComparingTo("12000.00");
        assertThat(jdbc.queryForObject("SELECT unit_price FROM quote_line WHERE quote_id = ?", BigDecimal.class,
                quoteId)).isEqualByComparingTo("6000.0000");
    }

    @Test
    void rejectsACustomerFromAnotherTenant() {
        assertThatThrownBy(() -> writer.create(cart(coCustomerId(), credit(), map("LEC-1L", 1))))
                .isInstanceOf(MissingCustomerException.class);
    }

    @Test
    void rejectsACartWithoutACustomerOrPaymentMethod() {
        assertThatThrownBy(() -> writer.create(cart(null, credit(), map("LEC-1L", 1))))
                .isInstanceOf(MissingCustomerException.class);
        assertThatThrownBy(() -> writer.create(cart(gtCustomerId(), null, map("LEC-1L", 1))))
                .isInstanceOf(MissingPaymentMethodException.class);
    }

    @Test
    void rejectsAnUnknownPaymentMethod() {
        assertThatThrownBy(() -> writer.create(cart(gtCustomerId(), 999999L, map("LEC-1L", 1))))
                .isInstanceOf(MissingPaymentMethodException.class);
    }

    @Test
    void rejectsACartTheEngineCannotPrice() {
        Cart cart = new Cart(gtTenantId(), gtCustomerId(), cash(), List.of(new CartLine(999999L, 1)));

        assertThatThrownBy(() -> writer.create(cart)).isInstanceOf(MissingPriceException.class);
    }

    @Test
    void rejectsACrossTenantCustomerAtTheDatabase() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO quote (public_id, tenant_id, customer_id, ruleset_id, payment_method_id, total, created_at, "
                        + "expires_at) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                UUID.randomUUID(), gtTenantId(), coCustomerId(), gtRulesetId(), cash(), new BigDecimal("1.00")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private long quoteId(CreatedQuote created) {
        return jdbc.queryForObject("SELECT id FROM quote WHERE public_id = ?", Long.class, created.publicId());
    }

    private BigDecimal quoteTotal(long quoteId) {
        return jdbc.queryForObject("SELECT total FROM quote WHERE id = ?", BigDecimal.class, quoteId);
    }

    private int discountCount(long quoteId) {
        return count("SELECT COUNT(*) FROM quote_line_discount d JOIN quote_line l ON l.id = d.quote_line_id "
                + "WHERE l.quote_id = ?", quoteId);
    }

    private int lineDiscountCount(long quoteId, String sku, String leverColumn) {
        return count("SELECT COUNT(*) FROM quote_line_discount d JOIN quote_line l ON l.id = d.quote_line_id "
                + "WHERE l.quote_id = ? AND l.product_id = ? AND d." + leverColumn + " IS NOT NULL",
                quoteId, gtProduct(sku));
    }

    private BigDecimal amount(long quoteId, String sku, String leverColumn) {
        return jdbc.queryForObject("SELECT d.amount FROM quote_line_discount d JOIN quote_line l ON l.id = d.quote_line_id "
                + "WHERE l.quote_id = ? AND l.product_id = ? AND d." + leverColumn + " IS NOT NULL",
                BigDecimal.class, quoteId, gtProduct(sku));
    }

    private <T> T quoteGift(long quoteId, String column, Class<T> type) {
        return jdbc.queryForObject("SELECT " + column + " FROM quote_gift WHERE quote_id = ?", type, quoteId);
    }

    private int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    private Long cash() {
        return paymentMethodId("CASH");
    }

    private Long credit() {
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

    private void give(long rulesetId, long conditionId, String giftSku, int qty, boolean taxable) {
        insert("INSERT INTO gift (ruleset_id, condition_id, gift_product_id, gift_qty, taxable) "
                + "VALUES (?, ?, ?, ?, ?)", rulesetId, conditionId, gtProduct(giftSku), qty, taxable);
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
