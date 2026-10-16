package com.marketplaceb2b.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.marketplaceb2b.model.ProductPrice;
import com.marketplaceb2b.model.RoundingMode;
import com.marketplaceb2b.model.Ruleset;
import com.marketplaceb2b.model.TaxRate;
import com.marketplaceb2b.persistence.repository.ProductPriceRepository;
import com.marketplaceb2b.persistence.repository.RulesetRepository;
import com.marketplaceb2b.persistence.repository.TaxRateRepository;

@Service
public class OrderPricingService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

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
        Ruleset ruleset = rulesets.currentForTenant(cart.tenantId(), clock.instant())
                .orElseThrow(() -> new NoCurrentRulesetException(cart.tenantId()));

        Set<Long> productIds = cart.lines().stream().map(CartLine::productId).collect(Collectors.toSet());
        Map<Long, ProductPrice> priceByProduct = productPrices.findByRulesetAndProducts(ruleset.id(), productIds)
                .stream().collect(Collectors.toMap(ProductPrice::productId, Function.identity()));
        Map<Long, BigDecimal> rateByCategory = taxRates.findByRuleset(ruleset.id()).stream()
                .collect(Collectors.toMap(TaxRate::taxCategoryId, TaxRate::rate));

        int scale = ruleset.currencyScale();
        java.math.RoundingMode mode = toJavaRounding(ruleset.roundingMode());

        List<LineBreakdown> lines = new ArrayList<>();
        BigDecimal grossSubtotal = BigDecimal.ZERO;
        BigDecimal totalBase = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (CartLine line : cart.lines()) {
            ProductPrice price = priceByProduct.get(line.productId());
            if (price == null) {
                throw new MissingPriceException(ruleset.id(), line.productId());
            }
            BigDecimal gross = price.unitPrice().multiply(BigDecimal.valueOf(line.qty())).setScale(scale, mode);
            BigDecimal taxableBase = gross;
            BigDecimal rate = rateByCategory.get(price.taxCategoryId());
            if (rate == null) {
                throw new MissingTaxRateException(ruleset.id(), price.taxCategoryId());
            }
            BigDecimal tax = taxableBase.multiply(rate).divide(HUNDRED, scale, mode);
            lines.add(new LineBreakdown(line.productId(), line.qty(), price.unitPrice(), gross, taxableBase, rate, tax));
            grossSubtotal = grossSubtotal.add(gross);
            totalBase = totalBase.add(taxableBase);
            totalTax = totalTax.add(tax);
        }

        BigDecimal total = totalBase.add(totalTax);
        return new PriceBreakdown(cart.tenantId(), ruleset.id(), ruleset.currency().trim(), List.copyOf(lines),
                List.of(), List.of(), grossSubtotal, BigDecimal.ZERO, totalBase, totalTax, total);
    }

    private static java.math.RoundingMode toJavaRounding(RoundingMode mode) {
        return switch (mode) {
            case HALF_UP -> java.math.RoundingMode.HALF_UP;
            case HALF_EVEN -> java.math.RoundingMode.HALF_EVEN;
        };
    }
}
