package com.marketplaceb2b.service;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketplaceb2b.dto.Cart;
import com.marketplaceb2b.dto.CreatedQuote;
import com.marketplaceb2b.dto.DiscountBreakdown;
import com.marketplaceb2b.dto.DiscountOrigin;
import com.marketplaceb2b.dto.LineBreakdown;
import com.marketplaceb2b.dto.PriceBreakdown;
import com.marketplaceb2b.model.Gift;
import com.marketplaceb2b.repository.CustomerRepository;
import com.marketplaceb2b.repository.GiftRepository;
import com.marketplaceb2b.repository.PersistenceAccess;
import com.marketplaceb2b.service.exception.MissingCustomerException;
import com.marketplaceb2b.service.exception.MissingPaymentMethodException;

@Service
public class QuoteWriter {

    private static final Duration TTL = Duration.ofHours(24);

    private final OrderPricingService pricing;
    private final CustomerRepository customers;
    private final GiftRepository gifts;
    private final PersistenceAccess access;
    private final Clock clock;

    public QuoteWriter(OrderPricingService pricing, CustomerRepository customers, GiftRepository gifts,
                       PersistenceAccess access, Clock clock) {
        this.pricing = pricing;
        this.customers = customers;
        this.gifts = gifts;
        this.access = access;
        this.clock = clock;
    }

    /** Prices the cart and persists the immutable quote, its lines and the applied discounts and gifts. */
    @Transactional
    public CreatedQuote create(Cart cart) {
        PriceBreakdown breakdown = pricing.price(cart);
        long tenantId = cart.tenantId();

        if (cart.customerId() == null || customers.find(tenantId, cart.customerId()).isEmpty()) {
            throw new MissingCustomerException(tenantId, cart.customerId());
        }
        if (cart.paymentMethodId() == null) {
            throw new MissingPaymentMethodException(tenantId, null);
        }

        Instant now = clock.instant();
        Instant expiresAt = now.plus(TTL);
        UUID publicId = UUID.randomUUID();
        long quoteId = insertQuote(publicId, cart, breakdown, now, expiresAt);

        Map<Long, Long> lineIds = insertLines(quoteId, breakdown);
        insertLineDiscounts(breakdown, lineIds);
        insertGifts(quoteId, breakdown);

        return new CreatedQuote(publicId, expiresAt, breakdown);
    }

    private long insertQuote(UUID publicId, Cart cart, PriceBreakdown breakdown, Instant now, Instant expiresAt) {
        return access.insertReturningId(
                "INSERT INTO quote (public_id, tenant_id, customer_id, ruleset_id, payment_method_id, total, created_at, "
                        + "expires_at) VALUES (:publicId, :tenantId, :customerId, :rulesetId, :paymentMethodId, :total, "
                        + ":createdAt, :expiresAt)",
                Map.of("publicId", publicId, "tenantId", cart.tenantId(), "customerId", cart.customerId(),
                        "rulesetId", breakdown.rulesetId(), "paymentMethodId", cart.paymentMethodId(),
                        "total", breakdown.total(), "createdAt", Timestamp.from(now),
                        "expiresAt", Timestamp.from(expiresAt)));
    }

    private Map<Long, Long> insertLines(long quoteId, PriceBreakdown breakdown) {
        Map<Long, Long> lineIds = new HashMap<>();
        for (LineBreakdown line : breakdown.lines()) {
            if (line.gift()) {
                continue;
            }
            long lineId = access.insertReturningId(
                    "INSERT INTO quote_line (quote_id, product_id, qty, unit_price, taxable_base, tax_rate, tax) "
                            + "VALUES (:quoteId, :productId, :qty, :unitPrice, :taxableBase, :taxRate, :tax)",
                    Map.of("quoteId", quoteId, "productId", line.productId(), "qty", line.qty(),
                            "unitPrice", line.unitPrice(), "taxableBase", line.taxableBase(),
                            "taxRate", line.taxRate(), "tax", line.tax()));
            lineIds.put(line.productId(), lineId);
        }
        return lineIds;
    }

    private void insertLineDiscounts(PriceBreakdown breakdown, Map<Long, Long> lineIds) {
        for (DiscountBreakdown discount : breakdown.discounts()) {
            Long lineId = lineIds.get(discount.productId());
            if (lineId == null || discount.origin() == DiscountOrigin.GIFT) {
                continue;
            }
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("lineId", lineId);
            params.put("comboId", discount.origin() == DiscountOrigin.COMBO ? discount.sourceId() : null);
            params.put("scaleId", discount.origin() == DiscountOrigin.SCALE ? discount.sourceId() : null);
            params.put("detail", discount.detail());
            params.put("amount", discount.amount());
            access.insert("INSERT INTO quote_line_discount (quote_line_id, combo_id, scale_discount_id, detail, amount) "
                    + "VALUES (:lineId, :comboId, :scaleId, :detail, :amount)", params);
        }
    }

    private void insertGifts(long quoteId, PriceBreakdown breakdown) {
        Map<Long, Gift> giftsById = new HashMap<>();
        for (Gift gift : gifts.findByRuleset(breakdown.rulesetId())) {
            giftsById.put(gift.id(), gift);
        }
        for (LineBreakdown line : breakdown.lines()) {
            if (!line.gift()) {
                continue;
            }
            DiscountBreakdown entry = breakdown.discounts().stream()
                    .filter(discount -> discount.origin() == DiscountOrigin.GIFT
                            && Objects.equals(discount.productId(), line.productId()))
                    .findFirst()
                    .orElseThrow();
            Gift gift = giftsById.get(entry.sourceId());
            boolean taxable = gift != null && gift.taxable();
            access.insert("INSERT INTO quote_gift (quote_id, gift_id, product_id, qty, taxable, gift_value, tax) "
                            + "VALUES (:quoteId, :giftId, :productId, :qty, :taxable, :value, :tax)",
                    Map.of("quoteId", quoteId, "giftId", entry.sourceId(), "productId", line.productId(),
                            "qty", line.qty(), "taxable", taxable, "value", line.gross(), "tax", line.tax()));
        }
    }
}
