package com.marketplaceb2b.service.pricing;

import java.math.BigDecimal;

import com.marketplaceb2b.model.Money;
import com.marketplaceb2b.model.Ruleset;

/**
 * The mutable state of one cart line while the levers run: the list gross is fixed, the levers add to
 * the line's discount and consume units, and the taxable base is the gross minus the discount.
 */
public final class PricingLine {

    private final long productId;
    private final int qty;
    private final BigDecimal unitPrice;
    private final BigDecimal rate;
    private final Money gross;
    private Money discount;
    private int consumedUnits;

    private PricingLine(long productId, int qty, BigDecimal unitPrice, BigDecimal rate, Money gross) {
        this.productId = productId;
        this.qty = qty;
        this.unitPrice = unitPrice;
        this.rate = rate;
        this.gross = gross;
        this.discount = Money.zero(gross.scale(), gross.roundingMode());
    }

    public static PricingLine of(long productId, int qty, BigDecimal unitPrice, BigDecimal rate, Ruleset ruleset) {
        Money gross = Money.of(unitPrice.multiply(BigDecimal.valueOf(qty)), ruleset);
        return new PricingLine(productId, qty, unitPrice, rate, gross);
    }

    public long productId() {
        return productId;
    }

    public int qty() {
        return qty;
    }

    public BigDecimal unitPrice() {
        return unitPrice;
    }

    public BigDecimal rate() {
        return rate;
    }

    public Money gross() {
        return gross;
    }

    public Money discount() {
        return discount;
    }

    public Money taxableBase() {
        return gross.minus(discount);
    }

    public int consumedUnits() {
        return consumedUnits;
    }

    public int availableUnits() {
        return qty - consumedUnits;
    }

    Money unitPriceMoney() {
        return Money.of(unitPrice, gross.scale(), gross.roundingMode());
    }

    void discountBy(Money amount) {
        discount = discount.plus(amount);
    }

    void consume(int units) {
        if (units < 0 || consumedUnits + units > qty) {
            throw new IllegalArgumentException(
                    "Cannot consume " + units + " units of product " + productId + "; only " + availableUnits()
                            + " available");
        }
        consumedUnits += units;
    }
}
