package com.marketplaceb2b.service.pricing;

import java.math.BigDecimal;

import com.marketplaceb2b.model.Money;
import com.marketplaceb2b.model.Ruleset;

/**
 * The mutable state of one line while the levers run: the list gross is fixed, the levers add to the
 * line's discount and consume units, and the taxable base is the gross minus the discount. A gift line
 * is marked and taxability decides whether its tax is charged on the gross (the documented exception).
 */
public final class PricingLine {

    private final long productId;
    private final int qty;
    private final BigDecimal unitPrice;
    private final BigDecimal rate;
    private final Money gross;
    private final boolean gift;
    private final boolean taxable;
    private Money discount;
    private int consumedUnits;

    private PricingLine(long productId, int qty, BigDecimal unitPrice, BigDecimal rate, Money gross, boolean gift,
                        boolean taxable) {
        this.productId = productId;
        this.qty = qty;
        this.unitPrice = unitPrice;
        this.rate = rate;
        this.gross = gross;
        this.gift = gift;
        this.taxable = taxable;
        this.discount = Money.zero(gross.scale(), gross.roundingMode());
    }

    public static PricingLine of(long productId, int qty, BigDecimal unitPrice, BigDecimal rate, Ruleset ruleset) {
        Money gross = Money.of(unitPrice.multiply(BigDecimal.valueOf(qty)), ruleset);
        return new PricingLine(productId, qty, unitPrice, rate, gross, false, false);
    }

    static PricingLine giftLine(long productId, int qty, BigDecimal unitPrice, BigDecimal rate, boolean taxable,
                                Ruleset ruleset) {
        Money gross = Money.of(unitPrice.multiply(BigDecimal.valueOf(qty)), ruleset);
        return new PricingLine(productId, qty, unitPrice, rate, gross, true, taxable);
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

    public boolean gift() {
        return gift;
    }

    public boolean taxable() {
        return taxable;
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
