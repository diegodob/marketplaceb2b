package com.marketplaceb2b.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.marketplaceb2b.model.CreditAccount;
import com.marketplaceb2b.model.IdempotencyKey;
import com.marketplaceb2b.model.LedgerEntryType;
import com.marketplaceb2b.model.OutboxDestination;
import com.marketplaceb2b.model.OutboxEventType;
import com.marketplaceb2b.model.Combo;
import com.marketplaceb2b.model.DiscountTargetType;
import com.marketplaceb2b.model.Gift;
import com.marketplaceb2b.model.PaymentMethod;
import com.marketplaceb2b.model.Product;
import com.marketplaceb2b.model.ProductFamily;
import com.marketplaceb2b.model.ProductPrice;
import com.marketplaceb2b.model.Quote;
import com.marketplaceb2b.model.Ruleset;
import com.marketplaceb2b.model.ScaleDiscount;
import com.marketplaceb2b.model.ScaleTier;
import com.marketplaceb2b.model.TaxRate;
import com.marketplaceb2b.model.SalesOrder;
import com.marketplaceb2b.support.AbstractIntegrationTest;

class RepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private PersistenceAccess access;
    @Autowired
    private QuoteRepository quotes;
    @Autowired
    private PaymentMethodRepository paymentMethods;
    @Autowired
    private CreditAccountRepository creditAccounts;
    @Autowired
    private IdempotencyKeyRepository idempotencyKeys;
    @Autowired
    private SalesOrderRepository salesOrders;
    @Autowired
    private CreditLedgerRepository creditLedger;
    @Autowired
    private OutboxRepository outbox;
    @Autowired
    private ProductFamilyRepository productFamilies;
    @Autowired
    private ProductRepository products;
    @Autowired
    private ScaleDiscountRepository scaleDiscounts;
    @Autowired
    private ComboRepository combos;
    @Autowired
    private GiftRepository gifts;
    @Autowired
    private RulesetRepository rulesets;
    @Autowired
    private ProductPriceRepository productPrices;
    @Autowired
    private TaxRateRepository taxRates;

    @Test
    void productFamilyRepositoryRoundTrips() {
        long tenant = gtTenantId();
        long id = productFamilies.insert(tenant, unique("Fam"));
        ProductFamily family = productFamilies.find(tenant, id).orElseThrow();
        assertThat(family.tenantId()).isEqualTo(tenant);
        assertThat(productFamilies.find(tenant + 999, id)).isEmpty();
    }

    @Test
    void scaleDiscountRepositoryLoadsSeedWithTiers() {
        List<ScaleDiscount> scales = scaleDiscounts.findByRuleset(gtRulesetId());
        assertThat(scales).hasSize(2);
        assertThat(scales).allSatisfy(scale -> assertThat(scale.priority()).isEqualTo(10));
        ScaleDiscount skuScale = scales.stream()
                .filter(scale -> scale.targetType() == DiscountTargetType.SKU).findFirst().orElseThrow();
        assertThat(skuScale.tiers()).extracting(ScaleTier::fromQty).containsExactly(12, 24);
        assertThat(scales.stream().filter(scale -> scale.targetType() == DiscountTargetType.FAMILY).findFirst()
                .orElseThrow().familyId()).isNotNull();
    }

    @Test
    void comboRepositoryLoadsSeedWithItems() {
        List<Combo> loaded = combos.findByRuleset(gtRulesetId());
        assertThat(loaded).hasSize(1);
        assertThat(loaded.get(0).comboPrice()).isEqualByComparingTo("20000.00");
        assertThat(loaded.get(0).priority()).isEqualTo(20);
        assertThat(loaded.get(0).items()).hasSize(2);
    }

    @Test
    void giftRepositoryLoadsSeedWithConditions() {
        List<Gift> loaded = gifts.findByRuleset(gtRulesetId());
        assertThat(loaded).hasSize(3);
        assertThat(loaded).allSatisfy(gift -> assertThat(gift.condition()).isNotNull());
        assertThat(loaded).allSatisfy(gift -> assertThat(gift.priority()).isEqualTo(5));
    }

    @Test
    void productRepositoryScopesByIdsTenant() {
        long guatemala = gtTenantId();
        long colombia = coTenantId();
        long gtGas = jdbc.queryForObject("SELECT id FROM product WHERE tenant_id = ? AND sku = 'GAS-500'",
                Long.class, guatemala);
        long coGas = jdbc.queryForObject("SELECT id FROM product WHERE tenant_id = ? AND sku = 'GAS-400'",
                Long.class, colombia);

        assertThat(products.findByIds(guatemala, List.of(gtGas, coGas)))
                .extracting(Product::id).containsExactly(gtGas);
        assertThat(products.findByIds(colombia, List.of(gtGas, coGas)))
                .extracting(Product::id).containsExactly(coGas);
    }

    @Test
    void pricingReadsResolveTheSeed() {
        Ruleset ruleset = rulesets.currentForTenant(gtTenantId(), Instant.now()).orElseThrow();
        assertThat(ruleset.currency().trim()).isEqualTo("GTQ");
        assertThat(taxRates.findByRuleset(ruleset.id())).isNotEmpty();

        long gas = jdbc.queryForObject("SELECT id FROM product WHERE sku = 'GAS-500'", Long.class);
        assertThat(productPrices.findByRulesetAndProducts(ruleset.id(), List.of(gas)))
                .singleElement()
                .satisfies(price -> assertThat(price.unitPrice()).isEqualByComparingTo("4000.00"));
    }

    @Test
    void giftRepositoryMapsTheTaxableFlag() {
        assertThat(gifts.findByRuleset(gtRulesetId()))
                .allSatisfy(gift -> assertThat(gift.taxable()).isFalse());

        long condition = insert("INSERT INTO gift_condition (product_id, family_id, min_qty, min_amount) "
                + "VALUES (NULL, NULL, NULL, 1000.00)");
        long giftId = insert("INSERT INTO gift (ruleset_id, condition_id, gift_product_id, gift_qty, taxable) "
                + "VALUES (?, ?, (SELECT id FROM product WHERE sku = 'AGU-500'), 1, TRUE)", gtRulesetId(), condition);

        Gift taxable = gifts.findByRuleset(gtRulesetId()).stream()
                .filter(gift -> gift.id() == giftId).findFirst().orElseThrow();
        assertThat(taxable.taxable()).isTrue();
    }

    @Test
    void insertReturningIdReturnsGeneratedKey() {
        long id = access.insertReturningId("INSERT INTO country (code, name) VALUES (:code, :name)",
                Map.of("code", "T9", "name", "Testland"));
        assertThat(id).isPositive();
        assertThat(jdbc.queryForObject("SELECT name FROM country WHERE id = ?", String.class, id))
                .isEqualTo("Testland");
    }

    @Test
    void quoteRepositoryLoadsTenantScopedQuote() {
        long tenant = gtTenantId();
        long quoteId = newQuote(tenant, gtCustomerId(), gtRulesetId(), paymentMethodId("CASH"), "1234.5600",
                Instant.now().plusSeconds(3600));
        Quote quote = quotes.find(tenant, quoteId).orElseThrow();
        assertThat(quote.id()).isEqualTo(quoteId);
        assertThat(quote.tenantId()).isEqualTo(tenant);
        assertThat(quote.total()).isEqualByComparingTo("1234.56");
        assertThat(quotes.find(tenant + 999, quoteId)).isEmpty();
    }

    @Test
    void paymentMethodRepositoryLoadsSeed() {
        PaymentMethod method = paymentMethods.find(gtTenantId(), paymentMethodId("CREDIT")).orElseThrow();
        assertThat(method.usesCredit()).isTrue();
    }

    @Test
    void paymentMethodLookupIsTenantScoped() {
        long method = paymentMethodId("CREDIT");
        assertThat(paymentMethods.find(gtTenantId(), method)).isPresent();
        assertThat(paymentMethods.find(coTenantId(), method)).isEmpty();
    }

    @Test
    void creditAccountLookupIsTenantScoped() {
        assertThat(creditAccounts.findByCustomerAndTenant(gtCustomerId(), gtTenantId())).isPresent();
        assertThat(creditAccounts.findByCustomerAndTenant(gtCustomerId(), coTenantId())).isEmpty();
    }

    @Test
    void creditAccountRepositoryLocksAndConsumes() {
        CreditAccount account = creditAccounts.lockByCustomerAndTenant(gtCustomerId(), gtTenantId()).orElseThrow();
        creditAccounts.consume(account.id(), new BigDecimal("1000.00"));
        BigDecimal available = jdbc.queryForObject("SELECT available FROM credit_account WHERE id = ?",
                BigDecimal.class, account.id());
        assertThat(available).isEqualByComparingTo(account.available().subtract(new BigDecimal("1000.00")));
    }

    @Test
    void idempotencyKeyRepositoryRoundTrips() {
        long customer = gtCustomerId();
        long order = salesOrders.insert(gtTenantId(), newQuote("10.00", Instant.now().plusSeconds(3600)), customer,
                "PUB-1", Instant.now());
        idempotencyKeys.insert(customer, "IDEM-R", order);
        IdempotencyKey key = idempotencyKeys.find(customer, "IDEM-R").orElseThrow();
        assertThat(key.salesOrderId()).isEqualTo(order);
    }

    @Test
    void salesOrderRepositoryInsertsAndReads() {
        long tenant = gtTenantId();
        long quote = newQuote("10.00", Instant.now().plusSeconds(3600));
        long orderId = salesOrders.insert(tenant, quote, gtCustomerId(), "PUB-2", Instant.now());
        SalesOrder order = salesOrders.findById(tenant, orderId).orElseThrow();
        assertThat(order.quoteId()).isEqualTo(quote);
        assertThat(order.publicNumber()).isEqualTo("PUB-2");
    }

    @Test
    void salesOrderLookupIsTenantScoped() {
        long tenant = gtTenantId();
        long order = salesOrders.insert(tenant, newQuote("10.00", Instant.now().plusSeconds(3600)), gtCustomerId(),
                "PUB-SCOPED", Instant.now());

        assertThat(salesOrders.findById(tenant, order)).isPresent();
        assertThat(salesOrders.findById(coTenantId(), order)).isEmpty();
    }

    @Test
    void ledgerAndOutboxRepositoriesInsert() {
        long customer = gtCustomerId();
        long account = jdbc.queryForObject("SELECT id FROM credit_account WHERE customer_id = ?", Long.class, customer);
        long order = salesOrders.insert(gtTenantId(), newQuote("10.00", Instant.now().plusSeconds(3600)), customer,
                "PUB-3", Instant.now());
        creditLedger.insert(account, order, new BigDecimal("-10.00"), LedgerEntryType.CONSUMPTION, Instant.now());
        outbox.insertPending(order, OutboxDestination.PUSH, OutboxEventType.ORDER_CONFIRMED, Instant.now());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM credit_ledger WHERE sales_order_id = ?", Integer.class, order))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM outbox WHERE sales_order_id = ?", Integer.class, order))
                .isEqualTo(1);
    }
}
