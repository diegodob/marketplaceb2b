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
import com.marketplaceb2b.dto.CreatedQuote;
import com.marketplaceb2b.dto.CreditFitReason;
import com.marketplaceb2b.dto.DiscountBreakdown;
import com.marketplaceb2b.dto.DiscountOrigin;
import com.marketplaceb2b.dto.LineBreakdown;
import com.marketplaceb2b.dto.OrderConfirmation;
import com.marketplaceb2b.dto.PriceBreakdown;
import com.marketplaceb2b.service.exception.InsufficientCreditException;
import com.marketplaceb2b.support.AbstractIntegrationTest;

/**
 * Golden set exigido por el enunciado: seis escenarios con la salida esperada completa fijada a
 * mano sobre el seed de Bebidas GT (líneas, descuentos, base, impuestos y total). No son snapshots:
 * si el comportamiento cambia, estos valores fallan y la actualización tiene que ser deliberada.
 *
 * <p>Los obsequios del seed se borran en cada test para que el resultado sea predecible; el caso 3
 * inserta el suyo y el caso 1 una escala adicional para LEC-1L (el seed solo trae escalas de GAS-500).
 * La suite es transaccional: cada test revierte sus inserciones.
 */
class GoldenSetTest extends AbstractIntegrationTest {

    @Autowired
    private OrderPricingService pricing;
    @Autowired
    private QuoteWriter writer;
    @Autowired
    private OrderConfirmationService confirmation;

    @BeforeEach
    void clearGifts() {
        jdbc.update("DELETE FROM gift WHERE ruleset_id = ?", gtRulesetId());
    }

    // 1. Un pedido con descuento en dos tramos.
    @Test
    void scaleDiscountAppliesTwoTiersInOneOrder() {
        long lecScale = insert("INSERT INTO scale_discount (ruleset_id, target_type, product_id, family_id, priority) "
                + "VALUES (?, 'SKU', ?, NULL, 10)", gtRulesetId(), productId("LEC-1L"));
        insert("INSERT INTO scale_tier (scale_discount_id, from_qty, percentage) VALUES (?, 10, 5)", lecScale);

        PriceBreakdown breakdown = pricing.price(cart(cash(), map("GAS-500", 24, "LEC-1L", 10)));

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
        PriceBreakdown breakdown = pricing.price(cart(cash(), map("GAS-500", 6, "AGU-500", 6)));

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
        long giftId = insert("INSERT INTO gift (ruleset_id, condition_id, gift_product_id, gift_qty, taxable, priority) "
                + "VALUES (?, ?, ?, 2, FALSE, 5)", gtRulesetId(), condition, productId("AGU-500"));

        PriceBreakdown breakdown = pricing.price(cart(cash(), map("GAS-500", 12)));

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
        PriceBreakdown breakdown = pricing.price(cart(cash(), map("GAS-500", 6, "AGU-500", 6)));

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

        PriceBreakdown breakdown = pricing.price(creditCart);
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

        CreatedQuote created = writer.create(creditCart);
        long quoteId = quoteId(created);

        assertThatThrownBy(() -> confirmation.confirm(gtTenantId(), quoteId, unique("KEY")))
                .isInstanceOf(InsufficientCreditException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sales_order WHERE quote_id = ?", Integer.class, quoteId))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM credit_ledger WHERE sales_order_id IN "
                + "(SELECT id FROM sales_order WHERE quote_id = ?)", Integer.class, quoteId)).isZero();
        assertThat(available(gtCustomerId())).isEqualByComparingTo("300000.00");
    }

    // 6. Regresión de la cotización: mismo carrito, cotizo y confirmo, los dos totales coinciden.
    @Test
    void quoteAndConfirmationTotalsMatch() {
        Cart cart = cart(credit(), map("GAS-500", 6, "AGU-500", 6));

        CreatedQuote created = writer.create(cart);
        long quoteId = quoteId(created);
        BigDecimal quotedTotal = created.breakdown().total();

        assertThat(quotedTotal).isEqualByComparingTo("22400.00");
        assertThat(quoteTotal(quoteId)).isEqualByComparingTo(quotedTotal);

        OrderConfirmation order = confirmation.confirm(gtTenantId(), quoteId, unique("KEY"));

        assertThat(order.replayed()).isFalse();
        assertThat(quoteTotal(quoteId)).isEqualByComparingTo(quotedTotal);
        assertThat(ledgerAmount(order.orderId()).abs()).isEqualByComparingTo(quotedTotal);
        assertThat(jdbc.queryForObject("SELECT quote_id FROM sales_order WHERE id = ?", Long.class, order.orderId()))
                .isEqualTo(quoteId);
        assertThat(jdbc.queryForObject("SELECT total FROM quote WHERE id = "
                        + "(SELECT quote_id FROM sales_order WHERE id = ?)", BigDecimal.class, order.orderId()))
                .isEqualByComparingTo(quotedTotal);
    }

    private long cash() {
        return paymentMethodId("CASH");
    }

    private long credit() {
        return paymentMethodId("CREDIT");
    }

    private Cart cart(long paymentMethodId, Map<String, Integer> skus) {
        List<CartLine> lines = new ArrayList<>();
        skus.forEach((sku, qty) -> lines.add(new CartLine(productId(sku), qty)));
        return new Cart(gtTenantId(), gtCustomerId(), paymentMethodId, List.copyOf(lines));
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

    private BigDecimal available(long customerId) {
        return jdbc.queryForObject("SELECT available FROM credit_account WHERE customer_id = ?", BigDecimal.class,
                customerId);
    }

    private long quoteId(CreatedQuote created) {
        return jdbc.queryForObject("SELECT id FROM quote WHERE public_id = ?", Long.class, created.publicId());
    }

    private BigDecimal quoteTotal(long quoteId) {
        return jdbc.queryForObject("SELECT total FROM quote WHERE id = ?", BigDecimal.class, quoteId);
    }

    private BigDecimal ledgerAmount(long orderId) {
        return jdbc.queryForObject("SELECT amount FROM credit_ledger WHERE sales_order_id = ?", BigDecimal.class,
                orderId);
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
