package com.marketplaceb2b.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.marketplaceb2b.dto.ApiError;
import com.marketplaceb2b.dto.Cart;
import com.marketplaceb2b.dto.CartLine;
import com.marketplaceb2b.dto.CreateOrderRequest;
import com.marketplaceb2b.dto.CreatedOrder;
import com.marketplaceb2b.dto.CreatedQuote;
import com.marketplaceb2b.dto.OrderView;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JdbcTemplate jdbc;

    private final List<Long> createdQuotes = new ArrayList<>();

    /** These tests commit through HTTP, so the rows they create must not leak into other suites. */
    @AfterEach
    void deleteCreatedQuotes() {
        for (long quoteId : createdQuotes) {
            jdbc.update("DELETE FROM outbox WHERE sales_order_id IN (SELECT id FROM sales_order WHERE quote_id = ?)", quoteId);
            jdbc.query("SELECT credit_account_id, amount FROM credit_ledger WHERE sales_order_id IN "
                            + "(SELECT id FROM sales_order WHERE quote_id = ?)",
                    rs -> {
                        jdbc.update("UPDATE credit_account SET available = available - ? WHERE id = ?",
                                rs.getBigDecimal("amount"), rs.getLong("credit_account_id"));
                    }, quoteId);
            jdbc.update("DELETE FROM credit_ledger WHERE sales_order_id IN (SELECT id FROM sales_order WHERE quote_id = ?)", quoteId);
            jdbc.update("DELETE FROM idempotency_key WHERE sales_order_id IN (SELECT id FROM sales_order WHERE quote_id = ?)", quoteId);
            jdbc.update("DELETE FROM sales_order WHERE quote_id = ?", quoteId);
            jdbc.update("DELETE FROM quote_line_discount WHERE quote_line_id IN (SELECT id FROM quote_line WHERE quote_id = ?)", quoteId);
            jdbc.update("DELETE FROM quote_gift WHERE quote_id = ?", quoteId);
            jdbc.update("DELETE FROM quote_line WHERE quote_id = ?", quoteId);
            jdbc.update("DELETE FROM quote WHERE id = ?", quoteId);
        }
        createdQuotes.clear();
    }

    @Test
    void confirmsAQuote() {
        CreatedQuote quote = newQuote("LEC-1L", 5);

        CreatedOrder order = confirm(quote.publicId(), "key-" + UUID.randomUUID(), 201);

        assertThat(order.number()).isNotBlank();
        assertThat(order.total()).isEqualByComparingTo("30000.00");
        assertThat(order.replayed()).isFalse();
    }

    @Test
    void replaysTheSameKeyWithTheSameOrder() {
        CreatedQuote quote = newQuote("LEC-1L", 5);
        String key = "key-" + UUID.randomUUID();

        CreatedOrder first = confirm(quote.publicId(), key, 201);
        CreatedOrder second = confirm(quote.publicId(), key, 201);

        assertThat(second.number()).isEqualTo(first.number());
        assertThat(second.replayed()).isTrue();
        assertThat(count("SELECT COUNT(*) FROM sales_order WHERE quote_id = ?", quoteId(quote))).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM credit_ledger WHERE sales_order_id = "
                + "(SELECT id FROM sales_order WHERE quote_id = ?)", quoteId(quote))).isEqualTo(1);
    }

    @Test
    void rejectsAKeyReusedForAnotherQuote() {
        CreatedQuote first = newQuote("LEC-1L", 1);
        CreatedQuote second = newQuote("LEC-1L", 1);
        String key = "key-" + UUID.randomUUID();

        confirm(first.publicId(), key, 201);
        confirmRejected(second.publicId(), key, 409, "IDEMPOTENCY_CONFLICT");
    }

    @Test
    void rejectsAQuoteAlreadyConfirmedWithAnotherKey() {
        CreatedQuote quote = newQuote("LEC-1L", 1);

        confirm(quote.publicId(), "key-a-" + UUID.randomUUID(), 201);
        confirmRejected(quote.publicId(), "key-b-" + UUID.randomUUID(), 409, "QUOTE_ALREADY_CONFIRMED");
    }

    @Test
    void rejectsAnUnknownQuote() {
        confirmRejected(UUID.randomUUID(), "key-" + UUID.randomUUID(), 404, "QUOTE_NOT_FOUND");
    }

    @Test
    void rejectsAnOrderThatExceedsTheCredit() {
        CreatedQuote quote = newQuote("LEC-1L", 100);

        confirmRejected(quote.publicId(), "key-" + UUID.randomUUID(), 422, "INSUFFICIENT_CREDIT");
        assertThat(count("SELECT COUNT(*) FROM sales_order WHERE quote_id = ?", quoteId(quote))).isZero();
    }

    @Test
    void readsTheOrderByItsNumber() {
        CreatedQuote quote = newQuote("LEC-1L", 5);
        CreatedOrder order = confirm(quote.publicId(), "key-" + UUID.randomUUID(), 201);

        webTestClient.get().uri("/api/v1/orders/{number}", order.number())
                .header("X-Tenant-Id", String.valueOf(tenantId()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(OrderView.class)
                .consumeWith(result -> {
                    OrderView view = result.getResponseBody();
                    assertThat(view).isNotNull();
                    assertThat(view.number()).isEqualTo(order.number());
                    assertThat(view.quotePublicId()).isEqualTo(quote.publicId());
                    assertThat(view.total()).isEqualByComparingTo("30000.00");
                    assertThat(view.lines()).hasSize(1);
                    assertThat(view.lines().get(0).unitPrice()).isEqualByComparingTo("6000");
                });
    }

    @Test
    void anotherTenantCannotReadTheOrder() {
        CreatedQuote quote = newQuote("LEC-1L", 1);
        CreatedOrder order = confirm(quote.publicId(), "key-" + UUID.randomUUID(), 201);

        webTestClient.get().uri("/api/v1/orders/{number}", order.number())
                .header("X-Tenant-Id", String.valueOf(colombiaTenantId()))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void doubleTapCreatesOneOrderAndReturnsTheSameResultToBoth() throws Exception {
        CreatedQuote quote = newQuote("LEC-1L", 1);
        String key = "key-" + UUID.randomUUID();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<CreatedOrder>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return confirm(quote.publicId(), key, 201);
                }));
            }
            start.countDown();
            CreatedOrder first = futures.get(0).get(30, TimeUnit.SECONDS);
            CreatedOrder second = futures.get(1).get(30, TimeUnit.SECONDS);

            assertThat(second.number()).isEqualTo(first.number());
            assertThat(count("SELECT COUNT(*) FROM sales_order WHERE quote_id = ?", quoteId(quote))).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM idempotency_key WHERE sales_order_id = "
                    + "(SELECT id FROM sales_order WHERE quote_id = ?)", quoteId(quote))).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM credit_ledger WHERE sales_order_id = "
                    + "(SELECT id FROM sales_order WHERE quote_id = ?)", quoteId(quote))).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM outbox WHERE sales_order_id = "
                    + "(SELECT id FROM sales_order WHERE quote_id = ?)", quoteId(quote))).isEqualTo(2);
        } finally {
            pool.shutdownNow();
        }
    }

    private CreatedQuote newQuote(String sku, int quantity) {
        Cart cart = new Cart(tenantId(), customerId(), creditMethodId(),
                List.of(new CartLine(productId(sku), quantity)));
        CreatedQuote quote = webTestClient.post().uri("/api/v1/quotes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(cart)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CreatedQuote.class)
                .returnResult()
                .getResponseBody();
        createdQuotes.add(quoteId(quote));
        return quote;
    }

    private CreatedOrder confirm(UUID quotePublicId, String key, int expectedStatus) {
        return webTestClient.post().uri("/api/v1/orders")
                .header("X-Tenant-Id", String.valueOf(tenantId()))
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateOrderRequest(quotePublicId))
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectBody(CreatedOrder.class)
                .returnResult()
                .getResponseBody();
    }

    private void confirmRejected(UUID quotePublicId, String key, int expectedStatus, String expectedCode) {
        webTestClient.post().uri("/api/v1/orders")
                .header("X-Tenant-Id", String.valueOf(tenantId()))
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateOrderRequest(quotePublicId))
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectBody(ApiError.class)
                .consumeWith(result -> assertThat(result.getResponseBody().code()).isEqualTo(expectedCode));
    }

    private long quoteId(CreatedQuote quote) {
        return jdbc.queryForObject("SELECT id FROM quote WHERE public_id = ?", Long.class, quote.publicId());
    }

    private int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    private long tenantId() {
        return jdbc.queryForObject("SELECT id FROM tenant WHERE name = 'Bebidas GT'", Long.class);
    }

    private long colombiaTenantId() {
        return jdbc.queryForObject("SELECT id FROM tenant WHERE name = 'Bebidas CO'", Long.class);
    }

    private long customerId() {
        return jdbc.queryForObject("SELECT id FROM customer WHERE name = 'Cliente 12'", Long.class);
    }

    private long creditMethodId() {
        return jdbc.queryForObject("SELECT id FROM payment_method WHERE tenant_id = ? AND code = 'CREDIT'",
                Long.class, tenantId());
    }

    private long productId(String sku) {
        return jdbc.queryForObject("SELECT id FROM product WHERE tenant_id = ? AND sku = ?", Long.class, tenantId(), sku);
    }
}
