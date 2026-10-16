package com.marketplaceb2b.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketplaceb2b.model.CreditAccount;
import com.marketplaceb2b.model.IdempotencyKey;
import com.marketplaceb2b.model.LedgerEntryType;
import com.marketplaceb2b.model.OutboxDestination;
import com.marketplaceb2b.model.OutboxEventType;
import com.marketplaceb2b.model.PaymentMethod;
import com.marketplaceb2b.model.Quote;
import com.marketplaceb2b.model.SalesOrder;
import com.marketplaceb2b.service.exception.CreditAccountRequiredException;
import com.marketplaceb2b.service.exception.IdempotencyConflictException;
import com.marketplaceb2b.service.exception.InsufficientCreditException;
import com.marketplaceb2b.service.exception.QuoteExpiredException;
import com.marketplaceb2b.service.exception.QuoteNotFoundException;
import com.marketplaceb2b.service.model.OrderConfirmation;
import com.marketplaceb2b.persistence.repository.CreditAccountRepository;
import com.marketplaceb2b.persistence.repository.CreditLedgerRepository;
import com.marketplaceb2b.persistence.repository.IdempotencyKeyRepository;
import com.marketplaceb2b.persistence.repository.OutboxRepository;
import com.marketplaceb2b.persistence.repository.PaymentMethodRepository;
import com.marketplaceb2b.persistence.repository.QuoteRepository;
import com.marketplaceb2b.persistence.repository.SalesOrderRepository;

@Service
public class OrderConfirmationService {

    private final QuoteRepository quotes;
    private final PaymentMethodRepository paymentMethods;
    private final CreditAccountRepository creditAccounts;
    private final IdempotencyKeyRepository idempotencyKeys;
    private final SalesOrderRepository salesOrders;
    private final CreditLedgerRepository creditLedger;
    private final OutboxRepository outbox;
    private final Clock clock;

    public OrderConfirmationService(QuoteRepository quotes, PaymentMethodRepository paymentMethods,
                                    CreditAccountRepository creditAccounts, IdempotencyKeyRepository idempotencyKeys,
                                    SalesOrderRepository salesOrders, CreditLedgerRepository creditLedger,
                                    OutboxRepository outbox, Clock clock) {
        this.quotes = quotes;
        this.paymentMethods = paymentMethods;
        this.creditAccounts = creditAccounts;
        this.idempotencyKeys = idempotencyKeys;
        this.salesOrders = salesOrders;
        this.creditLedger = creditLedger;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional
    public OrderConfirmation confirm(long tenantId, long quoteId, String idempotencyKey) {
        Quote quote = quotes.find(tenantId, quoteId).orElseThrow(() -> new QuoteNotFoundException(quoteId));
        long customerId = quote.customerId();

        if (!quote.expiresAt().isAfter(clock.instant())) {
            throw new QuoteExpiredException(quoteId);
        }

        Optional<IdempotencyKey> existing = idempotencyKeys.find(customerId, idempotencyKey);
        if (existing.isPresent()) {
            SalesOrder order = salesOrders.findById(existing.get().salesOrderId())
                    .orElseThrow(() -> new IllegalStateException("Order referenced by the idempotency key is missing"));
            if (!order.quoteId().equals(quoteId)) {
                throw new IdempotencyConflictException(idempotencyKey);
            }
            return OrderConfirmation.replay(order.id(), order.publicNumber());
        }

        PaymentMethod payment = paymentMethods.find(quote.paymentMethodId())
                .orElseThrow(() -> new IllegalStateException("Payment method not found: " + quote.paymentMethodId()));

        Instant now = clock.instant();
        Long creditAccountId = null;
        if (payment.usesCredit()) {
            CreditAccount account = creditAccounts.lockByCustomer(customerId)
                    .orElseThrow(() -> new CreditAccountRequiredException(customerId));
            if (account.available().compareTo(quote.total()) < 0) {
                throw new InsufficientCreditException(account.id(), account.available(), quote.total());
            }
            creditAccounts.consume(account.id(), quote.total());
            creditAccountId = account.id();
        }

        String publicNumber = UUID.randomUUID().toString();
        long orderId = salesOrders.insert(tenantId, quoteId, customerId, publicNumber, now);
        idempotencyKeys.insert(customerId, idempotencyKey, orderId);
        if (creditAccountId != null) {
            creditLedger.insert(creditAccountId, orderId, quote.total().negate(), LedgerEntryType.CONSUMPTION, now);
        }
        outbox.insertPending(orderId, OutboxDestination.PUSH, OutboxEventType.ORDER_CONFIRMED, now);
        outbox.insertPending(orderId, OutboxDestination.ERP, OutboxEventType.ORDER_CONFIRMED, now);

        return OrderConfirmation.confirmed(orderId, publicNumber);
    }
}
