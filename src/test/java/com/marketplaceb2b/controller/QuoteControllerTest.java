package com.marketplaceb2b.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.marketplaceb2b.dto.ApiError;
import com.marketplaceb2b.dto.Cart;
import com.marketplaceb2b.dto.CartLine;
import com.marketplaceb2b.dto.CreatedQuote;
import com.marketplaceb2b.dto.CreditFitReason;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QuoteControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void createsAQuote() {
        Cart cart = new Cart(tenantId(), customerId(), creditMethodId(),
                List.of(new CartLine(productId("LEC-1L"), 5)));

        webTestClient.post().uri("/api/v1/quotes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(cart)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody(CreatedQuote.class)
                .consumeWith(result -> {
                    CreatedQuote created = result.getResponseBody();
                    assertThat(created).isNotNull();
                    assertThat(created.publicId()).isNotNull();
                    assertThat(created.expiresAt()).isNotNull();
                    assertThat(created.breakdown().total()).isEqualByComparingTo("30000.00");
                    assertThat(created.breakdown().creditFit().reason()).isEqualTo(CreditFitReason.FITS);
                });
    }

    @Test
    void rejectsAnInvalidCart() {
        Cart cart = new Cart(tenantId(), customerId(), creditMethodId(), List.of());

        webTestClient.post().uri("/api/v1/quotes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(cart)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ApiError.class)
                .consumeWith(result -> assertThat(result.getResponseBody().code()).isEqualTo("INVALID_REQUEST"));
    }

    @Test
    void rejectsAnUnknownCustomer() {
        Cart cart = new Cart(tenantId(), 999999L, creditMethodId(),
                List.of(new CartLine(productId("LEC-1L"), 1)));

        webTestClient.post().uri("/api/v1/quotes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(cart)
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectBody(ApiError.class)
                .consumeWith(result -> assertThat(result.getResponseBody().code()).isEqualTo("CUSTOMER_NOT_FOUND"));
    }

    private long tenantId() {
        return jdbc.queryForObject("SELECT id FROM tenant WHERE name = 'Bebidas GT'", Long.class);
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
