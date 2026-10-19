package com.marketplaceb2b.service.pricing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.marketplaceb2b.dto.Cart;
import com.marketplaceb2b.dto.DiscountBreakdown;
import com.marketplaceb2b.dto.DiscountOrigin;
import com.marketplaceb2b.model.Combo;
import com.marketplaceb2b.model.Gift;
import com.marketplaceb2b.model.Money;
import com.marketplaceb2b.model.Product;
import com.marketplaceb2b.model.ProductPrice;
import com.marketplaceb2b.model.Ruleset;
import com.marketplaceb2b.model.ScaleDiscount;

/**
 * The cart, the ruleset and the loaded rules, plus the mutable accumulators the levers write to: the
 * per-line state and the discount entries. A gift adds a marked line that is never indexed by product,
 * so it cannot shadow a paid line for the same product.
 */
public final class PricingContext {

    private final Cart cart;
    private final Ruleset ruleset;
    private final Map<Long, ProductPrice> prices;
    private final Map<Long, BigDecimal> rates;
    private final List<Combo> combos;
    private final Map<Long, Combo> combosById;
    private final List<ScaleDiscount> scales;
    private final Map<Long, ScaleDiscount> scalesById;
    private final List<Gift> gifts;
    private final Map<Long, Gift> giftsById;
    private final List<PricingLine> lines = new ArrayList<>();
    private final Map<Long, PricingLine> linesByProduct;
    private final Map<Long, Product> products;
    private final List<DiscountBreakdown> discounts = new ArrayList<>();

    public PricingContext(Cart cart, Ruleset ruleset, Map<Long, ProductPrice> prices, Map<Long, BigDecimal> rates,
                          List<PricingLine> lines, List<Combo> combos, List<ScaleDiscount> scales,
                          List<Product> products, List<Gift> gifts) {
        this.cart = cart;
        this.ruleset = ruleset;
        this.prices = Map.copyOf(prices);
        this.rates = Map.copyOf(rates);
        this.combos = List.copyOf(combos);
        this.combosById = new LinkedHashMap<>();
        for (Combo combo : combos) {
            combosById.put(combo.id(), combo);
        }
        this.scales = List.copyOf(scales);
        this.scalesById = new LinkedHashMap<>();
        for (ScaleDiscount scale : scales) {
            scalesById.put(scale.id(), scale);
        }
        this.gifts = List.copyOf(gifts);
        this.giftsById = new LinkedHashMap<>();
        for (Gift gift : gifts) {
            giftsById.put(gift.id(), gift);
        }
        this.lines.addAll(lines);
        this.linesByProduct = new LinkedHashMap<>();
        for (PricingLine line : lines) {
            linesByProduct.put(line.productId(), line);
        }
        this.products = new LinkedHashMap<>();
        for (Product product : products) {
            this.products.put(product.id(), product);
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
        return List.copyOf(lines);
    }

    public List<Combo> combos() {
        return combos;
    }

    public List<ScaleDiscount> scales() {
        return scales;
    }

    public List<Gift> gifts() {
        return gifts;
    }

    public PricingLine line(long productId) {
        return linesByProduct.get(productId);
    }

    public Combo combo(long id) {
        return combosById.get(id);
    }

    public ScaleDiscount scale(long id) {
        return scalesById.get(id);
    }

    public Gift gift(long id) {
        return giftsById.get(id);
    }

    public Long familyOf(long productId) {
        Product product = products.get(productId);
        return product == null ? null : product.familyId();
    }

    public int availableUnits(long productId) {
        PricingLine line = linesByProduct.get(productId);
        return line == null ? 0 : line.availableUnits();
    }

    /** Appends a gift line without indexing it by product, so a paid line for the same product is kept. */
    public void addGiftLine(PricingLine line) {
        lines.add(line);
    }

    public void recordDiscount(long productId, DiscountOrigin origin, long sourceId, String detail, Money amount) {
        discounts.add(new DiscountBreakdown(productId, origin, sourceId, detail, amount.amount()));
    }

    public List<DiscountBreakdown> discounts() {
        return List.copyOf(discounts);
    }
}
