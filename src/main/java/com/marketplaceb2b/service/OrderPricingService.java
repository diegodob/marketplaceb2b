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

import com.marketplaceb2b.dto.Cart;
import com.marketplaceb2b.dto.CartLine;
import com.marketplaceb2b.dto.LineBreakdown;
import com.marketplaceb2b.dto.PriceBreakdown;
import com.marketplaceb2b.model.Combo;
import com.marketplaceb2b.model.Money;
import com.marketplaceb2b.model.Product;
import com.marketplaceb2b.model.ProductPrice;
import com.marketplaceb2b.model.Ruleset;
import com.marketplaceb2b.model.ScaleDiscount;
import com.marketplaceb2b.model.TaxRate;
import com.marketplaceb2b.repository.ComboRepository;
import com.marketplaceb2b.repository.ProductPriceRepository;
import com.marketplaceb2b.repository.ProductRepository;
import com.marketplaceb2b.repository.RulesetRepository;
import com.marketplaceb2b.repository.ScaleDiscountRepository;
import com.marketplaceb2b.repository.TaxRateRepository;
import com.marketplaceb2b.service.exception.MissingPriceException;
import com.marketplaceb2b.service.exception.MissingTaxRateException;
import com.marketplaceb2b.service.exception.NoCurrentRulesetException;
import com.marketplaceb2b.service.pricing.PricingContext;
import com.marketplaceb2b.service.pricing.PricingLine;
import com.marketplaceb2b.service.pricing.PricingPipeline;

@Service
public class OrderPricingService {

    private final RulesetRepository rulesets;
    private final ProductPriceRepository productPrices;
    private final ProductRepository products;
    private final TaxRateRepository taxRates;
    private final ComboRepository comboRepository;
    private final ScaleDiscountRepository scaleRepository;
    private final PricingPipeline pipeline;
    private final Clock clock;

    public OrderPricingService(RulesetRepository rulesets, ProductPriceRepository productPrices,
                               ProductRepository products, TaxRateRepository taxRates,
                               ComboRepository comboRepository, ScaleDiscountRepository scaleRepository,
                               PricingPipeline pipeline, Clock clock) {
        this.rulesets = rulesets;
        this.productPrices = productPrices;
        this.products = products;
        this.taxRates = taxRates;
        this.comboRepository = comboRepository;
        this.scaleRepository = scaleRepository;
        this.pipeline = pipeline;
        this.clock = clock;
    }

    public PriceBreakdown price(Cart cart) {
        Ruleset ruleset = currentRuleset(cart.tenantId());
        Set<Long> productIds = productIds(cart);
        Map<Long, ProductPrice> prices = pricesByProduct(ruleset, productIds);
        Map<Long, BigDecimal> rates = ratesByCategory(ruleset);
        List<PricingLine> lines = priceLines(cart, ruleset, prices, rates);
        List<Combo> combos = comboRepository.findByRuleset(ruleset.id());
        List<ScaleDiscount> scales = scaleRepository.findByRuleset(ruleset.id());
        List<Product> cartProducts = products.findByIds(cart.tenantId(), productIds);

        PricingContext context = new PricingContext(cart, ruleset, prices, rates, lines, combos, scales,
                cartProducts);
        pipeline.run(context);

        return breakdown(context);
    }

    private PriceBreakdown breakdown(PricingContext context) {
        Ruleset ruleset = context.ruleset();
        List<LineBreakdown> lines = new ArrayList<>();
        Money grossSubtotal = Money.zero(ruleset);
        Money totalDiscounts = Money.zero(ruleset);
        Money totalBase = Money.zero(ruleset);
        Money totalTax = Money.zero(ruleset);

        for (PricingLine line : context.lines()) {
            Money gross = line.gross();
            Money discount = line.discount();
            Money base = line.taxableBase();
            Money tax = base.percentage(line.rate());

            lines.add(new LineBreakdown(
                    line.productId(), line.qty(), line.unitPrice(),
                    gross.amount(), discount.amount(), base.amount(), line.rate(), tax.amount()));

            grossSubtotal = grossSubtotal.plus(gross);
            totalDiscounts = totalDiscounts.plus(discount);
            totalBase = totalBase.plus(base);
            totalTax = totalTax.plus(tax);
        }

        Money total = totalBase.plus(totalTax);
        return new PriceBreakdown(
                context.cart().tenantId(),
                ruleset.id(),
                ruleset.currency().trim(),
                List.copyOf(lines),
                context.discounts(),
                context.gifts(),
                grossSubtotal.amount(),
                totalDiscounts.amount(),
                totalBase.amount(),
                totalTax.amount(),
                total.amount());
    }

    private Ruleset currentRuleset(long tenantId) {
        return rulesets.currentForTenant(tenantId, clock.instant())
                .orElseThrow(() -> new NoCurrentRulesetException(tenantId));
    }

    private static Set<Long> productIds(Cart cart) {
        Set<Long> productIds = new LinkedHashSet<>();
        for (CartLine line : cart.lines()) {
            productIds.add(line.productId());
        }
        return productIds;
    }

    private Map<Long, ProductPrice> pricesByProduct(Ruleset ruleset, Set<Long> productIds) {
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

    private static List<PricingLine> priceLines(Cart cart, Ruleset ruleset, Map<Long, ProductPrice> prices,
                                                Map<Long, BigDecimal> rates) {
        List<PricingLine> lines = new ArrayList<>();
        for (CartLine line : cart.lines()) {
            ProductPrice price = prices.get(line.productId());
            if (price == null) {
                throw new MissingPriceException(ruleset.id(), line.productId());
            }
            BigDecimal rate = rates.get(price.taxCategoryId());
            if (rate == null) {
                throw new MissingTaxRateException(ruleset.id(), price.taxCategoryId());
            }
            lines.add(PricingLine.of(line.productId(), line.qty(), price.unitPrice(), rate, ruleset));
        }
        return lines;
    }
}
