package com.marketplaceb2b.service.pricing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.marketplaceb2b.dto.Cart;
import com.marketplaceb2b.dto.DiscountBreakdown;
import com.marketplaceb2b.dto.DiscountOrigin;
import com.marketplaceb2b.dto.GiftBreakdown;
import com.marketplaceb2b.model.Combo;
import com.marketplaceb2b.model.Money;
import com.marketplaceb2b.model.ProductPrice;
import com.marketplaceb2b.model.Ruleset;

/**
 * The cart, the ruleset and the loaded rules, plus the mutable accumulators the levers write to: the
 * per-line state, the discount entries and the gift entries.
 */
public final class PricingContext {

    private final Cart cart;
    private final Ruleset ruleset;
    private final Map<Long, ProductPrice> prices;
    private final Map<Long, BigDecimal> rates;
    private final List<Combo> combos;
    private final Map<Long, Combo> combosById;
    private final List<PricingLine> lines;
    private final Map<Long, PricingLine> linesByProduct;
    private final List<DiscountBreakdown> discounts = new ArrayList<>();
    private final List<GiftBreakdown> gifts = new ArrayList<>();

    public PricingContext(Cart cart, Ruleset ruleset, Map<Long, ProductPrice> prices, Map<Long, BigDecimal> rates,
                          List<PricingLine> lines, List<Combo> combos) {
        this.cart = cart;
        this.ruleset = ruleset;
        this.prices = Map.copyOf(prices);
        this.rates = Map.copyOf(rates);
        this.combos = List.copyOf(combos);
        this.combosById = new LinkedHashMap<>();
        for (Combo combo : combos) {
            combosById.put(combo.id(), combo);
        }
        this.lines = List.copyOf(lines);
        this.linesByProduct = new LinkedHashMap<>();
        for (PricingLine line : lines) {
            linesByProduct.put(line.productId(), line);
        }
    }

    public Cart cart() {
        return cart;
    }

    public Ruleset ruleset() {
        return ruleset;
    }

    public Map<Long, ProductPrice> prices() {
        return prices;
    }

    public Map<Long, BigDecimal> rates() {
        return rates;
    }

    public List<PricingLine> lines() {
        return lines;
    }

    public List<Combo> combos() {
        return combos;
    }

    public PricingLine line(long productId) {
        return linesByProduct.get(productId);
    }

    public Combo combo(long id) {
        return combosById.get(id);
    }

    public int availableUnits(long productId) {
        PricingLine line = linesByProduct.get(productId);
        return line == null ? 0 : line.availableUnits();
    }

    public void recordDiscount(DiscountOrigin origin, long sourceId, String detail, Money amount) {
        discounts.add(new DiscountBreakdown(origin, sourceId, detail, amount.amount()));
    }

    public void recordGift(long productId, int qty, boolean taxable, Money tax) {
        gifts.add(new GiftBreakdown(productId, qty, taxable, tax.amount()));
    }

    public List<DiscountBreakdown> discounts() {
        return List.copyOf(discounts);
    }

    public List<GiftBreakdown> gifts() {
        return List.copyOf(gifts);
    }
}
