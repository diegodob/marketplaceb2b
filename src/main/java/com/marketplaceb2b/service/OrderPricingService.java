package com.marketplaceb2b.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.marketplaceb2b.model.ProductPrice;
import com.marketplaceb2b.model.Ruleset;
import com.marketplaceb2b.model.TaxRate;
import com.marketplaceb2b.service.exception.MissingPriceException;
import com.marketplaceb2b.service.exception.MissingTaxRateException;
import com.marketplaceb2b.service.exception.NoCurrentRulesetException;
import com.marketplaceb2b.model.Money;
import com.marketplaceb2b.dto.Cart;
import com.marketplaceb2b.dto.CartLine;
import com.marketplaceb2b.dto.LineBreakdown;
import com.marketplaceb2b.dto.PriceBreakdown;
import com.marketplaceb2b.persistence.repository.ProductPriceRepository;
import com.marketplaceb2b.persistence.repository.RulesetRepository;
import com.marketplaceb2b.persistence.repository.TaxRateRepository;

@Service
public class OrderPricingService {

    private final RulesetRepository rulesets;
    private final ProductPriceRepository productPrices;
    private final TaxRateRepository taxRates;
    private final Clock clock;

    public OrderPricingService(RulesetRepository rulesets, ProductPriceRepository productPrices,
                               TaxRateRepository taxRates, Clock clock) {
        this.rulesets = rulesets;
        this.productPrices = productPrices;
        this.taxRates = taxRates;
        this.clock = clock;
    }

    public PriceBreakdown price(Cart cart) {
        Ruleset ruleset = currentRuleset(cart.tenantId());
        Map<Long, ProductPrice> prices = pricesByProduct(ruleset, cart);
        Map<Long, BigDecimal> rates = ratesByCategory(ruleset);

        List<LineBreakdown> lines = new ArrayList<>();
        Money grossSubtotal = Money.zero(ruleset);
        Money totalBase = Money.zero(ruleset);
        Money totalTax = Money.zero(ruleset);

        for (CartLine line : cart.lines()) {
            LinePricing priced = priceLine(line, ruleset, prices, rates);
            lines.add(priced.line());
            grossSubtotal = grossSubtotal.plus(priced.gross());
            totalBase = totalBase.plus(priced.taxableBase());
            totalTax = totalTax.plus(priced.tax());
        }

        Money total = totalBase.plus(totalTax);
        return new PriceBreakdown(
                cart.tenantId(),
                ruleset.id(),
                ruleset.currency().trim(),
                List.copyOf(lines),
                List.of(),
                List.of(),
                grossSubtotal.amount(),
                Money.zero(ruleset).amount(),
                totalBase.amount(),
                totalTax.amount(),
                total.amount());
    }

    private Ruleset currentRuleset(long tenantId) {
        return rulesets.currentForTenant(tenantId, clock.instant())
                .orElseThrow(() -> new NoCurrentRulesetException(tenantId));
    }

    private Map<Long, ProductPrice> pricesByProduct(Ruleset ruleset, Cart cart) {
        Set<Long> productIds = new LinkedHashSet<>();
        for (CartLine line : cart.lines()) {
            productIds.add(line.productId());
        }
        Map<Long, ProductPrice> byProduct = new HashMap<>();
        for (ProductPrice price : productPrices.findByRulesetAndProducts(ruleset.id(), productIds)) {
            byProduct.put(price.productId(), price);
        }
        return byProduct;
    }

    private Map<Long, BigDecimal> ratesByCategory(Ruleset ruleset) {
        Map<Long, BigDecimal> byCategory = new HashMap<>();
        for (TaxRate rate : taxRates.findByRuleset(ruleset.id())) {
            byCategory.put(rate.taxCategoryId(), rate.rate());
        }
        return byCategory;
    }

    private LinePricing priceLine(CartLine line, Ruleset ruleset, Map<Long, ProductPrice> prices,
                                  Map<Long, BigDecimal> rates) {
        ProductPrice price = prices.get(line.productId());
        if (price == null) {
            throw new MissingPriceException(ruleset.id(), line.productId());
        }
        BigDecimal rate = rates.get(price.taxCategoryId());
        if (rate == null) {
            throw new MissingTaxRateException(ruleset.id(), price.taxCategoryId());
        }

        Money gross = Money.of(listPrice(line, price), ruleset);
        Money taxableBase = gross;
        Money tax = taxableBase.percentage(rate);

        LineBreakdown breakdown = new LineBreakdown(
                line.productId(), line.qty(), price.unitPrice(),
                gross.amount(), taxableBase.amount(), rate, tax.amount());
        return new LinePricing(breakdown, gross, taxableBase, tax);
    }

    private static BigDecimal listPrice(CartLine line, ProductPrice price) {
        return price.unitPrice().multiply(BigDecimal.valueOf(line.qty()));
    }

    private record LinePricing(LineBreakdown line, Money gross, Money taxableBase, Money tax) {
    }
}
