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
import com.marketplaceb2b.dto.CreditFitReason;
import com.marketplaceb2b.dto.DiscountOrigin;
import com.marketplaceb2b.dto.PriceBreakdown;

/**
 * End-to-end over real HTTP. The request runs on another thread, outside any test transaction, so
 * the cases rely on the committed seed (there are no writes).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PricingControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JdbcTemplate jdbc;

    private long tenantId() {
        return jdbc.queryForObject("SELECT id FROM tenant WHERE name = 'Bebidas GT'", Long.class);
    }

    private long productId(String sku) {
        return jdbc.queryForObject("SELECT id FROM product WHERE tenant_id = ? AND sku = ?", Long.class, tenantId(), sku);
    }

    private long customerId() {
        return jdbc.queryForObject("SELECT id FROM customer WHERE name = 'Cliente 12'", Long.class);
    }

    private long creditMethodId() {
        return jdbc.queryForObject("SELECT id FROM payment_method WHERE tenant_id = ? AND code = 'CREDIT'",
                Long.class, tenantId());
    }

    @Test
    void pricesACartWithTheSeededCombo() {
        Cart cart = new Cart(tenantId(), null, null, List.of(
                new CartLine(productId("GAS-500"), 6),
                new CartLine(productId("AGU-500"), 6)));

        webTestClient.post().uri("/api/v1/pricing")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(cart)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody(PriceBreakdown.class)
                .consumeWith(result -> {
                    PriceBreakdown breakdown = result.getResponseBody();
                    assertThat(breakdown).isNotNull();
                    assertThat(breakdown.currency()).isEqualTo("GTQ");
                    assertThat(breakdown.lines()).hasSize(2);
                    assertThat(breakdown.discounts()).hasSize(2)
                            .allSatisfy(discount -> assertThat(discount.origin()).isEqualTo(DiscountOrigin.COMBO));
                    assertThat(breakdown.totalDiscounts()).isEqualByComparingTo("19000.00");
                    assertThat(breakdown.totalTaxableBase()).isEqualByComparingTo("20000.00");
                    assertThat(breakdown.totalTax()).isEqualByComparingTo("2400.00");
                    assertThat(breakdown.total()).isEqualByComparingTo("22400.00");
                });
    }

    @Test
    void pricesACartWithoutACombo() {
        Cart cart = new Cart(tenantId(), null, null, List.of(new CartLine(productId("GAS-500"), 1)));

        webTestClient.post().uri("/api/v1/pricing")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(cart)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PriceBreakdown.class)
                .consumeWith(result -> {
                    PriceBreakdown breakdown = result.getResponseBody();
                    assertThat(breakdown).isNotNull();
                    assertThat(breakdown.currency()).isEqualTo("GTQ");
                    assertThat(breakdown.discounts()).isEmpty();
                    assertThat(breakdown.total()).isEqualByComparingTo("4480.00");
                });
    }

    @Test
    void rejectsAProductWithoutPrice() {
        Cart cart = new Cart(tenantId(), null, null, List.of(new CartLine(999999L, 1)));

        webTestClient.post().uri("/api/v1/pricing")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(cart)
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectBody(ApiError.class)
                .consumeWith(result -> {
                    ApiError error = result.getResponseBody();
                    assertThat(error).isNotNull();
                    assertThat(error.code()).isEqualTo("PRODUCT_NOT_SELLABLE");
                    assertThat(error.message()).isEqualTo(
                            "Uno de los productos del carrito no está disponible para la venta.");
                    assertThat(error.message()).doesNotContain("ruleset", "tenant", "has no price", "999999");
                });
    }

    @Test
    void rejectsATenantWithoutPricing() {
        Cart cart = new Cart(999999L, null, null, List.of(new CartLine(productId("GAS-500"), 1)));

        webTestClient.post().uri("/api/v1/pricing")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(cart)
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody(ApiError.class)
                .consumeWith(result -> {
                    ApiError error = result.getResponseBody();
                    assertThat(error).isNotNull();
                    assertThat(error.code()).isEqualTo("STORE_PRICING_UNAVAILABLE");
                    assertThat(error.message()).isEqualTo("La tienda no tiene precios disponibles en este momento.");
                });
    }

    @Test
    void rejectsACartWithoutLines() {
        Cart cart = new Cart(tenantId(), null, null, List.of());

        webTestClient.post().uri("/api/v1/pricing")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(cart)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ApiError.class)
                .consumeWith(result -> {
                    ApiError error = result.getResponseBody();
                    assertThat(error).isNotNull();
                    assertThat(error.code()).isEqualTo("INVALID_REQUEST");
                    assertThat(error.details()).containsKey("lines");
                });
    }

    @Test
    void rejectsANonPositiveQuantity() {
        Cart cart = new Cart(tenantId(), null, null, List.of(new CartLine(productId("GAS-500"), 0)));

        webTestClient.post().uri("/api/v1/pricing")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(cart)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ApiError.class)
                .consumeWith(result -> {
                    ApiError error = result.getResponseBody();
                    assertThat(error).isNotNull();
                    assertThat(error.code()).isEqualTo("INVALID_REQUEST");
                });
    }

    @Test
    void reportsTheCreditFit() {
        Cart cart = new Cart(tenantId(), customerId(), creditMethodId(), List.of(new CartLine(productId("LEC-1L"), 5)));

        webTestClient.post().uri("/api/v1/pricing")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(cart)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PriceBreakdown.class)
                .consumeWith(result -> {
                    PriceBreakdown breakdown = result.getResponseBody();
                    assertThat(breakdown).isNotNull();
                    assertThat(breakdown.creditFit()).isNotNull();
                    assertThat(breakdown.creditFit().fits()).isTrue();
                    assertThat(breakdown.creditFit().reason()).isEqualTo(CreditFitReason.FITS);
                    assertThat(breakdown.creditFit().available()).isEqualByComparingTo("300000.00");
                    assertThat(breakdown.creditFit().total()).isEqualByComparingTo("30000.00");
                });
    }

    @Test
    void rejectsAnUnknownPaymentMethod() {
        Cart cart = new Cart(tenantId(), null, 999999L, List.of(new CartLine(productId("GAS-500"), 1)));

        webTestClient.post().uri("/api/v1/pricing")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(cart)
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectBody(ApiError.class)
                .consumeWith(result -> {
                    ApiError error = result.getResponseBody();
                    assertThat(error).isNotNull();
                    assertThat(error.code()).isEqualTo("PAYMENT_METHOD_NOT_FOUND");
                });
    }

    @Test
    void rejectsAMalformedBody() {
        webTestClient.post().uri("/api/v1/pricing")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{ not json")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ApiError.class)
                .consumeWith(result -> {
                    ApiError error = result.getResponseBody();
                    assertThat(error).isNotNull();
                    assertThat(error.code()).isEqualTo("INVALID_REQUEST");
                });
    }
}
