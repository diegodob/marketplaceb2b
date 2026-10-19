package com.marketplaceb2b.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.marketplaceb2b.dto.Cart;
import com.marketplaceb2b.dto.CartLine;
import com.marketplaceb2b.dto.CreatedQuote;
import com.marketplaceb2b.dto.OrderConfirmation;
import com.marketplaceb2b.repository.SalesOrderRepository;
import com.marketplaceb2b.service.exception.IdempotencyConflictException;
import com.marketplaceb2b.service.exception.InsufficientCreditException;
import com.marketplaceb2b.service.exception.QuoteExpiredException;
import com.marketplaceb2b.service.exception.QuoteNotFoundException;
import com.marketplaceb2b.support.AbstractIntegrationTest;

class QuoteToOrderTest extends AbstractIntegrationTest {

    @Autowired
    private QuoteWriter writer;
    @Autowired
    private OrderConfirmationService confirmation;
    @Autowired
    private SalesOrderRepository salesOrders;

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
    void aQuoteBecomesAnOrderAtItsStoredPriceAndSurvivesAPriceChange() {
        CreatedQuote created = writer.create(cart(gtCustomerId(), credit(), map("LEC-1L", 2)));
        long quoteId = quoteId(created);
        BigDecimal before = available(gtCustomerId());

        OrderConfirmation order = confirmation.confirm(gtTenantId(), quoteId, unique("KEY"));

        assertThat(quoteTotal(quoteId)).isEqualByComparingTo("12000.00");
        assertThat(ledgerAmount(order.orderId())).isEqualByComparingTo("-12000.00");
        assertThat(before.subtract(available(gtCustomerId()))).isEqualByComparingTo("12000.00");

        jdbc.update("UPDATE product_price SET unit_price = ? WHERE ruleset_id = ? AND product_id = ?",
                new BigDecimal("9999.0000"), gtRulesetId(), gtProduct("LEC-1L"));

        assertThat(quoteTotal(quoteId)).isEqualByComparingTo("12000.00");
        assertThat(jdbc.queryForObject("SELECT unit_price FROM quote_line WHERE quote_id = ?", BigDecimal.class,
                quoteId)).isEqualByComparingTo("6000.0000");
    }

    @Test
    void confirmsOnlyTheTenantsQuote() {
        long gtQuote = quoteId(writer.create(cart(gtCustomerId(), credit(), map("LEC-1L", 2))));
        long coQuote = quoteId(writer.create(coCart(map("GAS-400", 1))));

        OrderConfirmation gtOrder = confirmation.confirm(gtTenantId(), gtQuote, unique("KEY"));
        assertThat(gtOrder.replayed()).isFalse();

        assertThatThrownBy(() -> confirmation.confirm(gtTenantId(), coQuote, unique("KEY")))
                .isInstanceOf(QuoteNotFoundException.class);

        OrderConfirmation coOrder = confirmation.confirm(coTenantId(), coQuote, unique("KEY"));
        assertThat(salesOrders.findById(gtTenantId(), coOrder.orderId())).isEmpty();
        assertThat(salesOrders.findById(coTenantId(), coOrder.orderId())).isPresent();
    }

    @Test
    void consumesCreditOnceAndReplaysTheSameOrder() {
        long quoteId = quoteId(writer.create(cart(gtCustomerId(), credit(), map("LEC-1L", 2))));
        String key = unique("KEY");

        OrderConfirmation first = confirmation.confirm(gtTenantId(), quoteId, key);
        BigDecimal afterFirst = available(gtCustomerId());
        OrderConfirmation replay = confirmation.confirm(gtTenantId(), quoteId, key);

        assertThat(replay.replayed()).isTrue();
        assertThat(replay.orderId()).isEqualTo(first.orderId());
        assertThat(available(gtCustomerId())).isEqualByComparingTo(afterFirst);
        assertThat(count("SELECT COUNT(*) FROM credit_ledger WHERE sales_order_id = ?", first.orderId()))
                .isEqualTo(1);
    }

    @Test
    void aCashQuoteDoesNotConsumeCredit() {
        long quoteId = quoteId(writer.create(cart(gtCustomerId(), cash(), map("LEC-1L", 1))));
        BigDecimal before = available(gtCustomerId());

        OrderConfirmation order = confirmation.confirm(gtTenantId(), quoteId, unique("KEY"));

        assertThat(available(gtCustomerId())).isEqualByComparingTo(before);
        assertThat(count("SELECT COUNT(*) FROM credit_ledger WHERE sales_order_id = ?", order.orderId())).isZero();
    }

    @Test
    void insufficientCreditIsRejected() {
        setAvailable("0");
        long quoteId = quoteId(writer.create(cart(gtCustomerId(), credit(), map("LEC-1L", 1))));

        assertThatThrownBy(() -> confirmation.confirm(gtTenantId(), quoteId, unique("KEY")))
                .isInstanceOf(InsufficientCreditException.class);
    }

    @Test
    void expiredQuoteIsRejected() {
        long quoteId = quoteId(writer.create(cart(gtCustomerId(), cash(), map("LEC-1L", 1))));
        jdbc.update("UPDATE quote SET expires_at = ? WHERE id = ?",
                Timestamp.from(Instant.now().minusSeconds(60)), quoteId);

        assertThatThrownBy(() -> confirmation.confirm(gtTenantId(), quoteId, unique("KEY")))
                .isInstanceOf(QuoteExpiredException.class);
    }

    @Test
    void sameKeyWithAnotherQuoteIsRejected() {
        long first = quoteId(writer.create(cart(gtCustomerId(), cash(), map("LEC-1L", 1))));
        long second = quoteId(writer.create(cart(gtCustomerId(), cash(), map("GAS-500", 1))));
        String key = unique("KEY");

        confirmation.confirm(gtTenantId(), first, key);

        assertThatThrownBy(() -> confirmation.confirm(gtTenantId(), second, key))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void aQuoteProducesAtMostOneOrder() {
        long quoteId = quoteId(writer.create(cart(gtCustomerId(), cash(), map("LEC-1L", 1))));

        confirmation.confirm(gtTenantId(), quoteId, unique("KEY"));

        assertThatThrownBy(() -> confirmation.confirm(gtTenantId(), quoteId, unique("KEY")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private long quoteId(CreatedQuote created) {
        return jdbc.queryForObject("SELECT id FROM quote WHERE public_id = ?", Long.class, created.publicId());
    }

    private BigDecimal quoteTotal(long quoteId) {
        return jdbc.queryForObject("SELECT total FROM quote WHERE id = ?", BigDecimal.class, quoteId);
    }

    private BigDecimal ledgerAmount(long orderId) {
        return jdbc.queryForObject("SELECT amount FROM credit_ledger WHERE sales_order_id = ? "
                + "AND entry_type = 'CONSUMPTION'", BigDecimal.class, orderId);
    }

    private BigDecimal available(long customerId) {
        return jdbc.queryForObject("SELECT available FROM credit_account WHERE customer_id = ?", BigDecimal.class,
                customerId);
    }

    private void setAvailable(String amount) {
        jdbc.update("UPDATE credit_account SET available = ? WHERE customer_id = ?", new BigDecimal(amount),
                gtCustomerId());
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

    private Cart coCart(Map<String, Integer> skus) {
        List<CartLine> lines = new ArrayList<>();
        skus.forEach((sku, qty) -> lines.add(new CartLine(coProduct(sku), qty)));
        return new Cart(coTenantId(), coCustomerId(), paymentMethodId(coTenantId(), "CREDIT"), List.copyOf(lines));
    }

    private long gtProduct(String sku) {
        return jdbc.queryForObject("SELECT id FROM product WHERE tenant_id = ? AND sku = ?", Long.class, gtTenantId(),
                sku);
    }

    private long coProduct(String sku) {
        return jdbc.queryForObject("SELECT id FROM product WHERE tenant_id = ? AND sku = ?", Long.class, coTenantId(),
                sku);
    }

    private static Map<String, Integer> map(Object... pairs) {
        Map<String, Integer> entries = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            entries.put((String) pairs[i], (Integer) pairs[i + 1]);
        }
        return entries;
    }
}
