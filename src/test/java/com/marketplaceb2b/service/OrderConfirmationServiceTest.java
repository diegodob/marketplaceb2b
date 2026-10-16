package com.marketplaceb2b.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.marketplaceb2b.service.exception.IdempotencyConflictException;
import com.marketplaceb2b.service.exception.InsufficientCreditException;
import com.marketplaceb2b.service.exception.QuoteExpiredException;
import com.marketplaceb2b.service.model.OrderConfirmation;
import com.marketplaceb2b.support.AbstractIntegrationTest;

class OrderConfirmationServiceTest extends AbstractIntegrationTest {

    @Autowired
    private OrderConfirmationService service;

    @Test
    void createsOrderAndEnqueuesEventsForNonCreditMethod() {
        long tenant = arTenantId();
        long customer = arCustomerId();
        BigDecimal availableBefore = available(customer);
        long quote = newQuote(tenant, customer, arRulesetId(), paymentMethodId("CASH"), "1000.00",
                Instant.now().plusSeconds(3600));

        OrderConfirmation result = service.confirm(tenant, quote, unique("KEY"));

        assertThat(result.replayed()).isFalse();
        assertThat(result.publicNumber()).isNotBlank();
        assertThat(jdbc.queryForObject("SELECT quote_id FROM sales_order WHERE id = ?", Long.class, result.orderId()))
                .isEqualTo(quote);
        assertThat(count("SELECT COUNT(*) FROM outbox WHERE sales_order_id = ?", result.orderId())).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM credit_ledger WHERE sales_order_id = ?", result.orderId())).isZero();
        assertThat(available(customer)).isEqualByComparingTo(availableBefore);
    }

    @Test
    void consumesCreditWhenMethodUsesCredit() {
        long tenant = arTenantId();
        long customer = arCustomerId();
        BigDecimal before = available(customer);
        long quote = newQuote(tenant, customer, arRulesetId(), paymentMethodId("CREDIT"), "93070.00",
                Instant.now().plusSeconds(3600));

        OrderConfirmation result = service.confirm(tenant, quote, unique("KEY"));

        assertThat(before.subtract(available(customer))).isEqualByComparingTo("93070.00");
        BigDecimal amount = jdbc.queryForObject(
                "SELECT amount FROM credit_ledger WHERE sales_order_id = ? AND entry_type = 'CONSUMPTION'",
                BigDecimal.class, result.orderId());
        assertThat(amount).isEqualByComparingTo("-93070.00");
    }

    @Test
    void replayReturnsTheSameOrderWithoutChanges() {
        long tenant = arTenantId();
        long customer = arCustomerId();
        long quote = newQuote(tenant, customer, arRulesetId(), paymentMethodId("CREDIT"), "1000.00",
                Instant.now().plusSeconds(3600));
        String key = unique("KEY");

        OrderConfirmation first = service.confirm(tenant, quote, key);
        BigDecimal afterFirst = available(customer);

        OrderConfirmation second = service.confirm(tenant, quote, key);

        assertThat(second.replayed()).isTrue();
        assertThat(second.orderId()).isEqualTo(first.orderId());
        assertThat(count("SELECT COUNT(*) FROM sales_order WHERE quote_id = ?", quote)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM credit_ledger WHERE sales_order_id = ?", first.orderId())).isEqualTo(1);
        assertThat(available(customer)).isEqualByComparingTo(afterFirst);
    }

    @Test
    void sameKeyWithDifferentQuoteIsRejected() {
        long tenant = arTenantId();
        long customer = arCustomerId();
        long firstQuote = newQuote(tenant, customer, arRulesetId(), paymentMethodId("CREDIT"), "100.00",
                Instant.now().plusSeconds(3600));
        long secondQuote = newQuote(tenant, customer, arRulesetId(), paymentMethodId("CREDIT"), "200.00",
                Instant.now().plusSeconds(3600));
        String key = unique("KEY");

        service.confirm(tenant, firstQuote, key);
        assertThatThrownBy(() -> service.confirm(tenant, secondQuote, key))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void expiredQuoteIsRejected() {
        long tenant = arTenantId();
        long quote = newQuote(tenant, arCustomerId(), arRulesetId(), paymentMethodId("CASH"), "100.00",
                Instant.now().minusSeconds(60));

        assertThatThrownBy(() -> service.confirm(tenant, quote, unique("KEY")))
                .isInstanceOf(QuoteExpiredException.class);
        assertThat(count("SELECT COUNT(*) FROM sales_order WHERE quote_id = ?", quote)).isZero();
    }

    @Test
    void insufficientCreditIsRejectedWithoutLeavingAnOrder() {
        long tenant = arTenantId();
        long customer = arCustomerId();
        BigDecimal before = available(customer);
        long quote = newQuote(tenant, customer, arRulesetId(), paymentMethodId("CREDIT"), "400000.00",
                Instant.now().plusSeconds(3600));

        assertThatThrownBy(() -> service.confirm(tenant, quote, unique("KEY")))
                .isInstanceOf(InsufficientCreditException.class);
        assertThat(count("SELECT COUNT(*) FROM sales_order WHERE quote_id = ?", quote)).isZero();
        assertThat(available(customer)).isEqualByComparingTo(before);
    }

    @Test
    void aQuoteCannotProduceTwoOrders() {
        long tenant = arTenantId();
        long customer = arCustomerId();
        long quote = newQuote(tenant, customer, arRulesetId(), paymentMethodId("CASH"), "100.00",
                Instant.now().plusSeconds(3600));

        service.confirm(tenant, quote, unique("KEY"));
        assertThatThrownBy(() -> service.confirm(tenant, quote, unique("KEY")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void changingPricesAfterConfirmationDoesNotChangeTheOrder() {
        long tenant = arTenantId();
        long customer = arCustomerId();
        long quote = newQuote(tenant, customer, arRulesetId(), paymentMethodId("CASH"), "93070.00",
                Instant.now().plusSeconds(3600));

        OrderConfirmation confirmation = service.confirm(tenant, quote, unique("KEY"));

        jdbc.update("UPDATE product_price SET unit_price = 9999.0000 "
                + "WHERE product_id = (SELECT id FROM product WHERE sku = 'GAS-500')");

        BigDecimal orderAmount = jdbc.queryForObject("SELECT total FROM quote WHERE id = ?", BigDecimal.class, quote);
        assertThat(orderAmount).isEqualByComparingTo("93070.00");
        assertThat(count("SELECT COUNT(*) FROM sales_order WHERE id = ?", confirmation.orderId())).isEqualTo(1);
    }

    private BigDecimal available(long customerId) {
        return jdbc.queryForObject("SELECT available FROM credit_account WHERE customer_id = ?", BigDecimal.class,
                customerId);
    }

    private int count(String sql, long id) {
        return jdbc.queryForObject(sql, Integer.class, id);
    }
}
