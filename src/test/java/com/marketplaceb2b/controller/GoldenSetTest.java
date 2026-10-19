package com.marketplaceb2b.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.marketplaceb2b.dto.ApiError;
import com.marketplaceb2b.dto.Cart;
import com.marketplaceb2b.dto.CartLine;
import com.marketplaceb2b.dto.CreatedOrder;
import com.marketplaceb2b.dto.CreatedQuote;
import com.marketplaceb2b.dto.CreditFitReason;
import com.marketplaceb2b.dto.DiscountBreakdown;
import com.marketplaceb2b.dto.DiscountOrigin;
import com.marketplaceb2b.dto.LineBreakdown;
import com.marketplaceb2b.dto.OrderView;
import com.marketplaceb2b.dto.PriceBreakdown;

/**
 * Golden set exigido por el enunciado: seis escenarios a nivel de controller, con la salida HTTP
 * esperada fijada a mano sobre el seed de Bebidas GT. Se valida el contrato completo (validación,
 * serialización, errores) y no solo el servicio. No son snapshots: si cambia el comportamiento,
 * estos valores fallan y la actualización tiene que ser deliberada.
 *
 * <p>Los tests usan HTTP, así que commitean: cada uno limpia el grafo de cotización/pedido, restaura
 * el cupo consumido y revierte los fixtures que insertó. Los obsequios del seed se apartan en cada
 * test (y se restauran al final) para que el resultado sea predecible; el caso 3 inserta el suyo.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GoldenSetTest {

    private static final String TENANT = "X-Tenant-Id";

    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private JdbcTemplate jdbc;

    private final List<Long> quotes = new ArrayList<>();
    private final List<Long> gifts = new ArrayList<>();
    private final List<Long> conditions = new ArrayList<>();
    private final List<Long> scales = new ArrayList<>();
    private List<Map<String, Object>> seedGifts = List.of();

    @BeforeEach
    void detachSeedGifts() {
        seedGifts = new ArrayList<>(jdbc.queryForList(
                "SELECT ruleset_id, condition_id, gift_product_id, gift_qty, taxable, priority FROM gift"));
        jdbc.update("DELETE FROM gift");
    }

    @AfterEach
    void cleanup() {
        for (long quoteId : quotes) {
            restoreCredit(quoteId);
            deleteQuoteGraph(quoteId);
        }
        quotes.clear();
        for (long giftId : gifts) {
            jdbc.update("DELETE FROM gift WHERE id = ?", giftId);
        }
        gifts.clear();
        for (long conditionId : conditions) {
            jdbc.update("DELETE FROM gift_condition WHERE id = ?", conditionId);
        }
        conditions.clear();
        for (long scaleId : scales) {
            jdbc.update("DELETE FROM scale_tier WHERE scale_discount_id = ?", scaleId);
            jdbc.update("DELETE FROM scale_discount WHERE id = ?", scaleId);
        }
        scales.clear();
        for (Map<String, Object> gift : seedGifts) {
            jdbc.update("INSERT INTO gift (ruleset_id, condition_id, gift_product_id, gift_qty, taxable, priority) "
                            + "VALUES (?, ?, ?, ?, ?, ?)",
                    gift.get("ruleset_id"), gift.get("condition_id"), gift.get("gift_product_id"),
                    gift.get("gift_qty"), gift.get("taxable"), gift.get("priority"));
        }
    }

    // 1. Un pedido con descuento en dos tramos.
    @Test
    void scaleDiscountAppliesTwoTiersInOneOrder() {
        long lecScale = insertScale("LEC-1L", 10, 5);
        scales.add(lecScale);

        PriceBreakdown breakdown = post("/api/v1/pricing", cart(cash(), map("GAS-500", 24, "LEC-1L", 10)),
                Map.of(), 200, PriceBreakdown.class);

        LineBreakdown gas = line(breakdown, "GAS-500");
        assertThat(gas.qty()).isEqualTo(24);
        assertThat(gas.unitPrice()).isEqualByComparingTo("4000");
        assertThat(gas.gross()).isEqualByComparingTo("96000.00");
        assertThat(gas.discount()).isEqualByComparingTo("6720.00");
        assertThat(gas.taxableBase()).isEqualByComparingTo("89280.00");
        assertThat(gas.taxRate()).isEqualByComparingTo("12.0000");
        assertThat(gas.tax()).isEqualByComparingTo("10713.60");

        LineBreakdown lec = line(breakdown, "LEC-1L");
        assertThat(lec.qty()).isEqualTo(10);
        assertThat(lec.unitPrice()).isEqualByComparingTo("6000");
        assertThat(lec.gross()).isEqualByComparingTo("60000.00");
        assertThat(lec.discount()).isEqualByComparingTo("3000.00");
        assertThat(lec.taxableBase()).isEqualByComparingTo("57000.00");
        assertThat(lec.taxRate()).isEqualByComparingTo("0.0000");
        assertThat(lec.tax()).isEqualByComparingTo("0.00");

        assertThat(breakdown.discounts()).hasSize(2)
                .extracting(DiscountBreakdown::origin).containsOnly(DiscountOrigin.SCALE);
        assertThat(breakdown.discounts()).extracting(DiscountBreakdown::sourceId)
                .containsExactlyInAnyOrder(skuScaleId("GAS-500"), lecScale);
        assertThat(breakdown.discounts()).anySatisfy(discount -> {
            assertThat(discount.productId()).isEqualTo(productId("GAS-500"));
            assertThat(discount.amount()).isEqualByComparingTo("6720.00");
        });
        assertThat(breakdown.discounts()).anySatisfy(discount -> {
            assertThat(discount.productId()).isEqualTo(productId("LEC-1L"));
            assertThat(discount.amount()).isEqualByComparingTo("3000.00");
        });

        assertThat(breakdown.grossSubtotal()).isEqualByComparingTo("156000.00");
        assertThat(breakdown.totalDiscounts()).isEqualByComparingTo("9720.00");
        assertThat(breakdown.totalTaxableBase()).isEqualByComparingTo("146280.00");
        assertThat(breakdown.totalTax()).isEqualByComparingTo("10713.60");
        assertThat(breakdown.total()).isEqualByComparingTo("156993.60");
    }

    // 2. Un pedido con combo donde los productos que lo activan no aparecen como descuento suelto.
    @Test
    void comboIsNotAlsoReportedAsALooseDiscount() {
        PriceBreakdown breakdown = post("/api/v1/pricing", cart(cash(), map("GAS-500", 6, "AGU-500", 6)),
                Map.of(), 200, PriceBreakdown.class);

        LineBreakdown gas = line(breakdown, "GAS-500");
        assertThat(gas.qty()).isEqualTo(6);
        assertThat(gas.unitPrice()).isEqualByComparingTo("4000");
        assertThat(gas.gross()).isEqualByComparingTo("24000.00");
        assertThat(gas.discount()).isEqualByComparingTo("11692.31");
        assertThat(gas.taxableBase()).isEqualByComparingTo("12307.69");
        assertThat(gas.taxRate()).isEqualByComparingTo("12.0000");
        assertThat(gas.tax()).isEqualByComparingTo("1476.92");

        LineBreakdown agu = line(breakdown, "AGU-500");
        assertThat(agu.qty()).isEqualTo(6);
        assertThat(agu.unitPrice()).isEqualByComparingTo("2500");
        assertThat(agu.gross()).isEqualByComparingTo("15000.00");
        assertThat(agu.discount()).isEqualByComparingTo("7307.69");
        assertThat(agu.taxableBase()).isEqualByComparingTo("7692.31");
        assertThat(agu.taxRate()).isEqualByComparingTo("12.0000");
        assertThat(agu.tax()).isEqualByComparingTo("923.08");

        assertThat(breakdown.discounts()).hasSize(2)
                .extracting(DiscountBreakdown::origin).containsOnly(DiscountOrigin.COMBO);
        assertThat(breakdown.discounts()).allSatisfy(discount -> {
            assertThat(discount.sourceId()).isEqualTo(seedComboId());
            assertThat(discount.detail()).isEqualTo("Gaseosa + Agua x6");
        });
        assertThat(breakdown.discounts()).anySatisfy(discount -> {
            assertThat(discount.productId()).isEqualTo(productId("GAS-500"));
            assertThat(discount.amount()).isEqualByComparingTo("11692.31");
        });
        assertThat(breakdown.discounts()).anySatisfy(discount -> {
            assertThat(discount.productId()).isEqualTo(productId("AGU-500"));
            assertThat(discount.amount()).isEqualByComparingTo("7307.69");
        });

        BigDecimal sum = breakdown.discounts().stream()
                .map(DiscountBreakdown::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("19000.00");
        assertThat(sum).isEqualByComparingTo(breakdown.totalDiscounts());
        assertThat(breakdown.discounts().stream().map(DiscountBreakdown::amount).toList())
                .allSatisfy(amount -> assertThat(amount).isLessThan(new BigDecimal("19000.00")));

        assertThat(breakdown.grossSubtotal()).isEqualByComparingTo("39000.00");
        assertThat(breakdown.totalDiscounts()).isEqualByComparingTo("19000.00");
        assertThat(breakdown.totalTaxableBase()).isEqualByComparingTo("20000.00");
        assertThat(breakdown.totalTax()).isEqualByComparingTo("2400.00");
        assertThat(breakdown.total()).isEqualByComparingTo("22400.00");
    }

    // 3. Un pedido con obsequio: el producto gratis aparece con su cantidad en el desglose.
    @Test
    void giftAppearsWithItsQuantity() {
        long condition = insert("INSERT INTO gift_condition (product_id, family_id, min_qty, min_amount) "
                + "VALUES (?, NULL, 12, NULL)", productId("GAS-500"));
        conditions.add(condition);
        long giftId = insert("INSERT INTO gift (ruleset_id, condition_id, gift_product_id, gift_qty, taxable, priority) "
                + "VALUES (?, ?, ?, 2, FALSE, 5)", gtRulesetId(), condition, productId("AGU-500"));
        gifts.add(giftId);

        PriceBreakdown breakdown = post("/api/v1/pricing", cart(cash(), map("GAS-500", 12)),
                Map.of(), 200, PriceBreakdown.class);

        LineBreakdown paid = line(breakdown, "GAS-500");
        assertThat(paid.qty()).isEqualTo(12);
        assertThat(paid.gross()).isEqualByComparingTo("48000.00");
        assertThat(paid.discount()).isEqualByComparingTo("1440.00");
        assertThat(paid.taxableBase()).isEqualByComparingTo("46560.00");
        assertThat(paid.tax()).isEqualByComparingTo("5587.20");

        LineBreakdown gift = giftLine(breakdown, "AGU-500");
        assertThat(gift.qty()).isEqualTo(2);
        assertThat(gift.gift()).isTrue();
        assertThat(gift.unitPrice()).isEqualByComparingTo("2500");
        assertThat(gift.gross()).isEqualByComparingTo("5000.00");
        assertThat(gift.discount()).isEqualByComparingTo("5000.00");
        assertThat(gift.taxableBase()).isEqualByComparingTo("0.00");
        assertThat(gift.taxRate()).isEqualByComparingTo("12.0000");
        assertThat(gift.tax()).isEqualByComparingTo("0.00");

        assertThat(breakdown.discounts()).hasSize(2);
        assertThat(breakdown.discounts()).anySatisfy(discount -> {
            assertThat(discount.origin()).isEqualTo(DiscountOrigin.SCALE);
            assertThat(discount.sourceId()).isEqualTo(skuScaleId("GAS-500"));
            assertThat(discount.amount()).isEqualByComparingTo("1440.00");
        });
        assertThat(breakdown.discounts()).anySatisfy(discount -> {
            assertThat(discount.origin()).isEqualTo(DiscountOrigin.GIFT);
            assertThat(discount.sourceId()).isEqualTo(giftId);
            assertThat(discount.productId()).isEqualTo(productId("AGU-500"));
            assertThat(discount.amount()).isEqualByComparingTo("5000.00");
        });

        assertThat(breakdown.grossSubtotal()).isEqualByComparingTo("53000.00");
        assertThat(breakdown.totalDiscounts()).isEqualByComparingTo("6440.00");
        assertThat(breakdown.totalTaxableBase()).isEqualByComparingTo("46560.00");
        assertThat(breakdown.totalTax()).isEqualByComparingTo("5587.20");
        assertThat(breakdown.total()).isEqualByComparingTo("52147.20");
    }

    // 4. Un combo con impuestos: el impuesto entra en el total.
    @Test
    void comboWithTaxesIncludesTheTaxInTheTotal() {
        PriceBreakdown breakdown = post("/api/v1/pricing", cart(cash(), map("GAS-500", 6, "AGU-500", 6)),
                Map.of(), 200, PriceBreakdown.class);

        assertThat(line(breakdown, "GAS-500").taxRate()).isEqualByComparingTo("12.0000");
        assertThat(line(breakdown, "GAS-500").taxableBase()).isEqualByComparingTo("12307.69");
        assertThat(line(breakdown, "GAS-500").tax()).isEqualByComparingTo("1476.92");
        assertThat(line(breakdown, "AGU-500").taxRate()).isEqualByComparingTo("12.0000");
        assertThat(line(breakdown, "AGU-500").taxableBase()).isEqualByComparingTo("7692.31");
        assertThat(line(breakdown, "AGU-500").tax()).isEqualByComparingTo("923.08");

        assertThat(breakdown.totalTaxableBase()).isEqualByComparingTo("20000.00");
        assertThat(breakdown.totalTax()).isEqualByComparingTo("2400.00");
        assertThat(breakdown.total()).isEqualByComparingTo("22400.00");
        assertThat(breakdown.total()).isEqualByComparingTo(
                breakdown.totalTaxableBase().add(breakdown.totalTax()));
    }

    // 5. Un pedido a crédito que excede el cupo.
    @Test
    void creditOrderAboveTheLimitIsRejected() {
        Cart creditCart = cart(credit(), map("GAS-500", 80));

        PriceBreakdown breakdown = post("/api/v1/pricing", creditCart, Map.of(), 200, PriceBreakdown.class);
        LineBreakdown gas = line(breakdown, "GAS-500");
        assertThat(gas.qty()).isEqualTo(80);
        assertThat(gas.gross()).isEqualByComparingTo("320000.00");
        assertThat(gas.discount()).isEqualByComparingTo("22400.00");
        assertThat(gas.taxableBase()).isEqualByComparingTo("297600.00");
        assertThat(gas.tax()).isEqualByComparingTo("35712.00");

        assertThat(breakdown.grossSubtotal()).isEqualByComparingTo("320000.00");
        assertThat(breakdown.totalDiscounts()).isEqualByComparingTo("22400.00");
        assertThat(breakdown.totalTaxableBase()).isEqualByComparingTo("297600.00");
        assertThat(breakdown.totalTax()).isEqualByComparingTo("35712.00");
        assertThat(breakdown.total()).isEqualByComparingTo("333312.00");

        assertThat(breakdown.creditFit().fits()).isFalse();
        assertThat(breakdown.creditFit().reason()).isEqualTo(CreditFitReason.INSUFFICIENT_CREDIT);
        assertThat(breakdown.creditFit().available()).isEqualByComparingTo("300000.00");
        assertThat(breakdown.creditFit().total()).isEqualByComparingTo("333312.00");

        CreatedQuote created = post("/api/v1/quotes", creditCart, Map.of(), 201, CreatedQuote.class);
        long quoteId = trackQuote(created);

        ApiError error = post("/api/v1/orders", Map.of("quotePublicId", created.publicId()),
                Map.of(TENANT, "1", "Idempotency-Key", "golden-" + UUID.randomUUID()), 422, ApiError.class);

        assertThat(error.code()).isEqualTo("INSUFFICIENT_CREDIT");
        assertThat(count("SELECT COUNT(*) FROM sales_order WHERE quote_id = ?", quoteId)).isZero();
        assertThat(count("SELECT COUNT(*) FROM credit_ledger WHERE sales_order_id IN "
                + "(SELECT id FROM sales_order WHERE quote_id = ?)", quoteId)).isZero();
        assertThat(available()).isEqualByComparingTo("300000.00");
    }

    // 6. Regresión de la cotización: mismo carrito, cotizo y confirmo, los dos totales coinciden.
    @Test
    void quoteAndConfirmationTotalsMatch() {
        Cart cart = cart(credit(), map("GAS-500", 6, "AGU-500", 6));

        CreatedQuote created = post("/api/v1/quotes", cart, Map.of(), 201, CreatedQuote.class);
        long quoteId = trackQuote(created);
        BigDecimal quotedTotal = created.breakdown().total();

        assertThat(quotedTotal).isEqualByComparingTo("22400.00");
        assertThat(quoteTotal(quoteId)).isEqualByComparingTo(quotedTotal);

        CreatedOrder order = post("/api/v1/orders", Map.of("quotePublicId", created.publicId()),
                Map.of(TENANT, "1", "Idempotency-Key", "golden-" + UUID.randomUUID()), 201, CreatedOrder.class);

        assertThat(order.replayed()).isFalse();
        assertThat(order.total()).isEqualByComparingTo(quotedTotal);
        assertThat(quoteTotal(quoteId)).isEqualByComparingTo(quotedTotal);
        assertThat(ledgerAmount(quoteId).abs()).isEqualByComparingTo(quotedTotal);

        OrderView view = get("/api/v1/orders/" + order.number(), Map.of(TENANT, "1"), 200, OrderView.class);
        assertThat(view.quotePublicId()).isEqualTo(created.publicId());
        assertThat(view.total()).isEqualByComparingTo(quotedTotal);
        assertThat(view.lines()).hasSize(2);
        assertThat(view.lines()).extracting(line -> line.unitPrice())
                .anySatisfy(price -> assertThat(price).isEqualByComparingTo("4000"))
                .anySatisfy(price -> assertThat(price).isEqualByComparingTo("2500"));
    }

    private <T> T post(String path, Object body, Map<String, String> headers, int status, Class<T> type) {
        WebTestClient.RequestBodySpec spec = webTestClient.post().uri(path)
                .contentType(MediaType.APPLICATION_JSON);
        headers.forEach(spec::header);
        return spec.bodyValue(body)
                .exchange()
                .expectStatus().isEqualTo(status)
                .expectBody(type)
                .returnResult()
                .getResponseBody();
    }

    private <T> T get(String path, Map<String, String> headers, int status, Class<T> type) {
        WebTestClient.RequestHeadersSpec<?> spec = webTestClient.get().uri(path);
        headers.forEach(spec::header);
        return spec.exchange()
                .expectStatus().isEqualTo(status)
                .expectBody(type)
                .returnResult()
                .getResponseBody();
    }

    private long trackQuote(CreatedQuote created) {
        long quoteId = jdbc.queryForObject("SELECT id FROM quote WHERE public_id = ?", Long.class, created.publicId());
        quotes.add(quoteId);
        return quoteId;
    }

    private void restoreCredit(long quoteId) {
        jdbc.query("SELECT credit_account_id, amount FROM credit_ledger WHERE sales_order_id IN "
                        + "(SELECT id FROM sales_order WHERE quote_id = ?)",
                (java.sql.ResultSet rs) -> {
                    jdbc.update("UPDATE credit_account SET available = available - ? WHERE id = ?",
                            rs.getBigDecimal("amount"), rs.getLong("credit_account_id"));
                }, quoteId);
    }

    private void deleteQuoteGraph(long quoteId) {
        jdbc.update("DELETE FROM outbox WHERE sales_order_id IN (SELECT id FROM sales_order WHERE quote_id = ?)", quoteId);
        jdbc.update("DELETE FROM credit_ledger WHERE sales_order_id IN (SELECT id FROM sales_order WHERE quote_id = ?)", quoteId);
        jdbc.update("DELETE FROM idempotency_key WHERE sales_order_id IN (SELECT id FROM sales_order WHERE quote_id = ?)", quoteId);
        jdbc.update("DELETE FROM sales_order WHERE quote_id = ?", quoteId);
        jdbc.update("DELETE FROM quote_line_discount WHERE quote_line_id IN "
                + "(SELECT id FROM quote_line WHERE quote_id = ?)", quoteId);
        jdbc.update("DELETE FROM quote_gift WHERE quote_id = ?", quoteId);
        jdbc.update("DELETE FROM quote_line WHERE quote_id = ?", quoteId);
        jdbc.update("DELETE FROM quote WHERE id = ?", quoteId);
    }

    private long insertScale(String sku, int fromQty, int percentage) {
        long id = insert("INSERT INTO scale_discount (ruleset_id, target_type, product_id, family_id, priority) "
                + "VALUES (?, 'SKU', ?, NULL, 10)", gtRulesetId(), productId(sku));
        insert("INSERT INTO scale_tier (scale_discount_id, from_qty, percentage) VALUES (?, ?, ?)",
                id, fromQty, new BigDecimal(percentage));
        return id;
    }

    private long insert(String sql, Object... args) {
        org.springframework.jdbc.support.KeyHolder keys = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(connection -> {
            java.sql.PreparedStatement statement = connection.prepareStatement(sql,
                    java.sql.Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) {
                statement.setObject(i + 1, args[i]);
            }
            return statement;
        }, keys);
        Number key = keys.getKey();
        return key == null ? 0L : key.longValue();
    }

    private Cart cart(long paymentMethodId, Map<String, Integer> skus) {
        List<CartLine> lines = new ArrayList<>();
        skus.forEach((sku, qty) -> lines.add(new CartLine(productId(sku), qty)));
        return new Cart(gtTenantId(), gtCustomerId(), paymentMethodId, List.copyOf(lines));
    }

    private long cash() {
        return paymentMethodId("CASH");
    }

    private long credit() {
        return paymentMethodId("CREDIT");
    }

    private long paymentMethodId(String code) {
        return jdbc.queryForObject("SELECT id FROM payment_method WHERE tenant_id = ? AND code = ?", Long.class,
                gtTenantId(), code);
    }

    private long gtTenantId() {
        return jdbc.queryForObject("SELECT id FROM tenant WHERE name = 'Bebidas GT'", Long.class);
    }

    private long gtCustomerId() {
        return jdbc.queryForObject("SELECT id FROM customer WHERE name = 'Cliente 12'", Long.class);
    }

    private long gtRulesetId() {
        return jdbc.queryForObject("SELECT id FROM ruleset WHERE version = 'GT-2026-10-01.1'", Long.class);
    }

    private long productId(String sku) {
        return jdbc.queryForObject("SELECT id FROM product WHERE tenant_id = ? AND sku = ?", Long.class,
                gtTenantId(), sku);
    }

    private long skuScaleId(String sku) {
        return jdbc.queryForObject("SELECT id FROM scale_discount WHERE ruleset_id = ? AND target_type = 'SKU' "
                + "AND product_id = ?", Long.class, gtRulesetId(), productId(sku));
    }

    private long seedComboId() {
        return jdbc.queryForObject("SELECT id FROM combo WHERE ruleset_id = ? AND name = 'Gaseosa + Agua x6'",
                Long.class, gtRulesetId());
    }

    private BigDecimal available() {
        return jdbc.queryForObject("SELECT available FROM credit_account WHERE customer_id = ?", BigDecimal.class,
                gtCustomerId());
    }

    private BigDecimal quoteTotal(long quoteId) {
        return jdbc.queryForObject("SELECT total FROM quote WHERE id = ?", BigDecimal.class, quoteId);
    }

    private BigDecimal ledgerAmount(long quoteId) {
        return jdbc.queryForObject("SELECT amount FROM credit_ledger WHERE sales_order_id IN "
                + "(SELECT id FROM sales_order WHERE quote_id = ?)", BigDecimal.class, quoteId);
    }

    private int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    private LineBreakdown line(PriceBreakdown breakdown, String sku) {
        long productId = productId(sku);
        return breakdown.lines().stream()
                .filter(line -> line.productId() == productId && !line.gift())
                .findFirst().orElseThrow();
    }

    private LineBreakdown giftLine(PriceBreakdown breakdown, String sku) {
        long productId = productId(sku);
        return breakdown.lines().stream()
                .filter(line -> line.productId() == productId && line.gift())
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
