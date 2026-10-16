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
        RoundingMode mode = ruleset.roundingMode();

        List<LineBreakdown> lines = new ArrayList<>();
        Money grossSubtotal = Money.zero(scale, mode);
        Money totalBase = Money.zero(scale, mode);
        Money totalTax = Money.zero(scale, mode);

        for (CartLine line : cart.lines()) {
            ProductPrice price = priceByProduct.get(line.productId());
            if (price == null) {
                throw new MissingPriceException(ruleset.id(), line.productId());
            }
            BigDecimal rate = rateByCategory.get(price.taxCategoryId());
            if (rate == null) {
                throw new MissingTaxRateException(ruleset.id(), price.taxCategoryId());
            }
            Money gross = Money.of(price.unitPrice(), scale, mode).times(line.qty());
            Money taxableBase = gross;
            Money tax = taxableBase.percentage(rate);
            lines.add(new LineBreakdown(line.productId(), line.qty(), price.unitPrice(), gross.amount(),
                    taxableBase.amount(), rate, tax.amount()));
            grossSubtotal = grossSubtotal.plus(gross);
            totalBase = totalBase.plus(taxableBase);
            totalTax = totalTax.plus(tax);
        }

        Money total = totalBase.plus(totalTax);
        return new PriceBreakdown(cart.tenantId(), ruleset.id(), ruleset.currency().trim(), List.copyOf(lines),
                List.of(), List.of(), grossSubtotal.amount(), Money.zero(scale, mode).amount(), totalBase.amount(),
                totalTax.amount(), total.amount());
    }
}
