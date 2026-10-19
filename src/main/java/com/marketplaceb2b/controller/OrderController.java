package com.marketplaceb2b.controller;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.marketplaceb2b.dto.CreateOrderRequest;
import com.marketplaceb2b.dto.CreatedOrder;
import com.marketplaceb2b.dto.OrderConfirmation;
import com.marketplaceb2b.dto.OrderView;
import com.marketplaceb2b.model.IdempotencyKey;
import com.marketplaceb2b.model.Quote;
import com.marketplaceb2b.model.SalesOrder;
import com.marketplaceb2b.repository.IdempotencyKeyRepository;
import com.marketplaceb2b.repository.QuoteLineRepository;
import com.marketplaceb2b.repository.QuoteRepository;
import com.marketplaceb2b.repository.SalesOrderRepository;
import com.marketplaceb2b.service.OrderConfirmationService;
import com.marketplaceb2b.service.exception.IdempotencyConflictException;
import com.marketplaceb2b.service.exception.QuoteAlreadyConfirmedException;
import com.marketplaceb2b.service.exception.QuoteNotFoundException;

import jakarta.validation.Valid;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/v1")
public class OrderController {

    private final OrderConfirmationService confirmations;
    private final QuoteRepository quotes;
    private final QuoteLineRepository lines;
    private final SalesOrderRepository orders;
    private final IdempotencyKeyRepository idempotencyKeys;

    public OrderController(OrderConfirmationService confirmations, QuoteRepository quotes, QuoteLineRepository lines,
                           SalesOrderRepository orders, IdempotencyKeyRepository idempotencyKeys) {
        this.confirmations = confirmations;
        this.quotes = quotes;
        this.lines = lines;
        this.orders = orders;
        this.idempotencyKeys = idempotencyKeys;
    }

    @PostMapping("/orders")
    public Mono<ResponseEntity<CreatedOrder>> confirm(
            @RequestHeader("X-Tenant-Id") long tenantId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {
        return Mono.fromCallable(() -> confirmNow(tenantId, idempotencyKey, request))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/orders/{number}")
    public Mono<ResponseEntity<OrderView>> find(
            @RequestHeader("X-Tenant-Id") long tenantId,
            @PathVariable String number) {
        return Mono.fromCallable(() -> orders.findByPublicNumber(tenantId, number)
                        .map(order -> ResponseEntity.ok(view(order)))
                        .orElseGet(() -> ResponseEntity.notFound().build()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private ResponseEntity<CreatedOrder> confirmNow(long tenantId, String idempotencyKey, CreateOrderRequest request) {
        Quote quote = quotes.findByPublicId(tenantId, request.quotePublicId())
                .orElseThrow(() -> new QuoteNotFoundException(request.quotePublicId().toString()));
        try {
            OrderConfirmation confirmation = confirmations.confirm(tenantId, quote.id(), idempotencyKey);
            return created(quote, confirmation.publicNumber(), confirmation.replayed());
        } catch (DataIntegrityViolationException race) {
            return resolveRace(quote, idempotencyKey);
        }
    }

    /**
     * The losing insert re-reads the winner without re-running the confirmation: the same key
     * replays the winner's order, and another key means the quote already had an order.
     */
    private ResponseEntity<CreatedOrder> resolveRace(Quote quote, String idempotencyKey) {
        Optional<IdempotencyKey> winner = idempotencyKeys.find(quote.customerId(), idempotencyKey);
        if (winner.isPresent()) {
            SalesOrder order = orders.findById(quote.tenantId(), winner.get().salesOrderId())
                    .orElseThrow(() -> new IllegalStateException("Order referenced by the idempotency key is missing"));
            if (!order.quoteId().equals(quote.id())) {
                throw new IdempotencyConflictException(idempotencyKey);
            }
            return created(quote, order.publicNumber(), true);
        }
        throw new QuoteAlreadyConfirmedException(quote.id());
    }

    private ResponseEntity<CreatedOrder> created(Quote quote, String number, boolean replayed) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreatedOrder(number, quote.total(), replayed));
    }

    private OrderView view(SalesOrder order) {
        Quote quote = quotes.find(order.tenantId(), order.quoteId())
                .orElseThrow(() -> new IllegalStateException("Quote of the order is missing"));
        return new OrderView(order.publicNumber(), quote.publicId(), quote.total(), order.createdAt(),
                lines.findByQuote(order.tenantId(), order.quoteId()));
    }
}
