# Proposal

## Why

The confirmation domain service (`OrderConfirmationService`: idempotency key, cupo lock, immutable snapshot, outbox) is implemented and tested, but it has no HTTP surface. A client cannot confirm a quote, retry the confirmation or look a confirmed order up — which is what A2 asks for (`POST /orders` with an idempotency key, and a queryable order).

## What Changes

- Add `POST /api/v1/orders`: `X-Tenant-Id` and `Idempotency-Key` headers, the quote's public id in the body -> **201** with the order number and the total. It delegates to `OrderConfirmationService`; it does not re-price and does not trust client amounts.
- **Replay**: the same key with the same quote returns the same 201 and the same order. The same key with another quote -> **409** `IDEMPOTENCY_CONFLICT`. The same quote with another key -> **409** `QUOTE_ALREADY_CONFIRMED`.
- **Double tap**: when the insert loses the race, the endpoint catches the `DataIntegrityViolationException`, re-reads the order by the key in a fresh read-only transaction and returns the same 201, so the invariant (one order, one consumption) holds and both requests see the same result.
- Add `GET /api/v1/orders/{number}` (tenant-scoped): the order and its lines -> **200**; another tenant or an unknown number -> **404**.
- Map the confirmation errors: unknown quote -> **404**, insufficient credit / no account / expired quote -> **422**, conflicts -> **409**, missing tenant -> **401**, missing key/body -> **400**.

## Capabilities

### New Capabilities

- `order-confirmation-api`: the HTTP contract for confirming and reading orders.

### Modified Capabilities

- (none)

## Impact

- New: `OrderController`, the request/response DTOs, the error mapping in `ApiExceptionHandler`, and tenant-scoped `QuoteRepository.findByPublicId` / `SalesOrderRepository.findByQuote`.
- `OrderConfirmationService` keeps its behavior; the controller adds the conflict re-read.
- Tests: the endpoint E2E, the concurrent double tap, the tenant isolation, the quote -> order flow.
- The README: the order endpoints, the pending list and the error table.
