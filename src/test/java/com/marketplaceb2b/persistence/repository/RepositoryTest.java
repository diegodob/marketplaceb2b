package com.marketplaceb2b.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.marketplaceb2b.model.CreditAccount;
import com.marketplaceb2b.model.IdempotencyKey;
import com.marketplaceb2b.model.LedgerEntryType;
import com.marketplaceb2b.model.OutboxDestination;
import com.marketplaceb2b.model.OutboxEventType;
import com.marketplaceb2b.model.PaymentMethod;
import com.marketplaceb2b.model.ProductFamily;
import com.marketplaceb2b.model.Quote;
import com.marketplaceb2b.model.SalesOrder;
import com.marketplaceb2b.persistence.PersistenceAccess;
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

    @Test
    void productFamilyRepositoryRoundTrips() {
        long tenant = arTenantId();
        long id = productFamilies.insert(tenant, unique("Fam"));
        ProductFamily family = productFamilies.find(tenant, id).orElseThrow();
        assertThat(family.tenantId()).isEqualTo(tenant);
        assertThat(productFamilies.find(tenant + 999, id)).isEmpty();
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
        long tenant = arTenantId();
        long quoteId = newQuote(tenant, arCustomerId(), arRulesetId(), paymentMethodId("CASH"), "1234.5600",
                Instant.now().plusSeconds(3600));
        Quote quote = quotes.find(tenant, quoteId).orElseThrow();
        assertThat(quote.id()).isEqualTo(quoteId);
        assertThat(quote.tenantId()).isEqualTo(tenant);
        assertThat(quote.total()).isEqualByComparingTo("1234.56");
        assertThat(quotes.find(tenant + 999, quoteId)).isEmpty();
    }

    @Test
    void paymentMethodRepositoryLoadsSeed() {
        PaymentMethod method = paymentMethods.find(paymentMethodId("CREDIT")).orElseThrow();
        assertThat(method.usesCredit()).isTrue();
    }

    @Test
    void creditAccountRepositoryLocksAndConsumes() {
        CreditAccount account = creditAccounts.lockByCustomer(arCustomerId()).orElseThrow();
        creditAccounts.consume(account.id(), new BigDecimal("1000.00"));
        BigDecimal available = jdbc.queryForObject("SELECT available FROM credit_account WHERE id = ?",
                BigDecimal.class, account.id());
        assertThat(available).isEqualByComparingTo(account.available().subtract(new BigDecimal("1000.00")));
    }

    @Test
    void idempotencyKeyRepositoryRoundTrips() {
        long customer = arCustomerId();
        long order = salesOrders.insert(arTenantId(), newQuote("10.00", Instant.now().plusSeconds(3600)), customer,
                "PUB-1", Instant.now());
        idempotencyKeys.insert(customer, "IDEM-R", order);
        IdempotencyKey key = idempotencyKeys.find(customer, "IDEM-R").orElseThrow();
        assertThat(key.salesOrderId()).isEqualTo(order);
    }

    @Test
    void salesOrderRepositoryInsertsAndReads() {
        long tenant = arTenantId();
        long quote = newQuote("10.00", Instant.now().plusSeconds(3600));
        long orderId = salesOrders.insert(tenant, quote, arCustomerId(), "PUB-2", Instant.now());
        SalesOrder order = salesOrders.findById(orderId).orElseThrow();
        assertThat(order.quoteId()).isEqualTo(quote);
        assertThat(order.publicNumber()).isEqualTo("PUB-2");
    }

    @Test
    void ledgerAndOutboxRepositoriesInsert() {
        long customer = arCustomerId();
        long account = jdbc.queryForObject("SELECT id FROM credit_account WHERE customer_id = ?", Long.class, customer);
        long order = salesOrders.insert(arTenantId(), newQuote("10.00", Instant.now().plusSeconds(3600)), customer,
                "PUB-3", Instant.now());
        creditLedger.insert(account, order, new BigDecimal("-10.00"), LedgerEntryType.CONSUMPTION, Instant.now());
        outbox.insertPending(order, OutboxDestination.PUSH, OutboxEventType.ORDER_CONFIRMED, Instant.now());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM credit_ledger WHERE sales_order_id = ?", Integer.class, order))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM outbox WHERE sales_order_id = ?", Integer.class, order))
                .isEqualTo(1);
    }
}
