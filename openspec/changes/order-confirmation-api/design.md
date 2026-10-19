# Design

## Context

- `OrderConfirmationService.confirm(tenant, quoteId, key)` re-reads the quote and its stored total, locks the credit account, inserts the order, the lines, the consumption and the outbox event, and replays by the key.
- `pricing-api` already defines the tenant header, the public-id convention and the error shape.
- `quote.public_id` is unique and is the client's reference; `sales_order` already has `idempotency_key UNIQUE (customer_id, idempotency_key)` and one order per quote.

## Goals

- Confirm over HTTP without exposing the internal ids.
- Never two orders or two consumptions, also under the double tap.
- The retry returns the same result.
- A confirmed order is queryable, tenant-scoped.

## Non-Goals

- Delivering the outbox (a separate change).
- Re-pricing at confirmation.
- Pagination or search over orders.

## Decisions

- **The endpoint delegates; it never prices.** The body carries the quote's public id only; the service re-reads the quote and its frozen total.
- **The key is the `Idempotency-Key` header** (the take-home's contract); a missing key is a 400.
- **The response is 201 `{number, total, replayed}`**, where `number` is the tenant-scoped **public number** (`sales_order.public_number`), the client-facing reference; `replayed` is `true` when the order already existed.
- **The replay is a 201** with the same body, matching "the retry returns the same result".
- **The conflicts are 409**: the same key with another quote (`IDEMPOTENCY_CONFLICT`) and the same quote with another key (`QUOTE_ALREADY_CONFIRMED`, via `SalesOrderRepository.findByQuote`).
- **The double tap resolves by the database constraint**: the loser catches the `DataIntegrityViolationException`, re-reads by the key in a fresh read-only transaction (the controller is not transactional, so the failed insert does not poison it) and returns the replay; if the key is not found it is the quote conflict (409).
- **The GET is tenant-scoped** on `(tenant_id, public_number)`; another tenant gets a 404, so existence does not leak.
- **The quote's public id is exposed in the GET** for the client's reference; the quote's internal id stays internal.

## Risks / Trade-offs

- The 409 for the same quote with another key is explicit in the contract, not a silent replay.
- The fresh read after a race costs one extra query, only under contention.

## Migration Plan

- None: `sales_order`, `order_line`, `idempotency_key` and `outbox` already exist.

## Open Questions

- None.
