# Design

## Context

`model` holds the immutable domain types and `persistence-layer` added the Flyway schema plus `PersistenceAccess` (a thin JDBC wrapper with an append-only guard). There is no orchestration layer yet. See proposal.md for motivation and `specs/order-confirmation/spec.md` for the behaviour contract.

## Goals / Non-Goals

**Goals:**
- A single `OrderConfirmationService.confirm(...)` that is idempotent and atomic.
- All data access behind repositories; the service holds no SQL.
- Reuse the quote's frozen values; no pricing.

**Non-Goals:**
- Quote creation / pricing, catalog or customer CRUD.
- Outbox relay delivery.
- HTTP endpoints.

## Decisions

**Repositories, not SQL in the service.** Each aggregate gets a repository in `com.marketplaceb2b.persistence.repository` that owns its SQL and row mapping (`QuoteRepository`, `PaymentMethodRepository`, `CreditAccountRepository`, `IdempotencyKeyRepository`, `SalesOrderRepository`, `CreditLedgerRepository`, `OutboxRepository`). The service depends on those interfaces. Alternative considered: inline SQL inside the service — rejected by the team; data access belongs in a dedicated layer.

**Thin repositories over `PersistenceAccess`.** Repositories delegate query/write to the existing `PersistenceAccess` (or `NamedParameterJdbcTemplate`), keeping the append-only guard in one place. `PersistenceAccess` gains `insertReturningId(...)` (via `KeyHolder`) so an insert can return the generated `id`.

**Transaction boundary in the service.** `OrderConfirmationService.confirm(...)` is `@Transactional`; the repositories' statements run on the same connection, so the order, idempotency key, ledger entry, credit update and outbox rows commit together. Spring Boot auto-configures a `DataSourceTransactionManager` (no JPA involved).

**Idempotency by lookup, backed by the unique constraint.** The service first looks up `(customer_id, idem_key)`: a hit returns the stored order. The `UNIQUE (customer_id, idem_key)` is the safety net: a concurrent confirmation with the same key fails the insert rather than duplicating. Automatically resolving that race by returning the winner's order is deferred — it needs a retry outside the failed transaction; for now the losing attempt surfaces an error.

**Credit locking.** When the payment method uses credit, the account row is read `FOR UPDATE` so concurrent confirmations of the same customer serialise and cannot over-consume the balance.

**Result type.** `confirm(...)` returns an `OrderConfirmation` value carrying the order id, public number and whether it was a replay.

## Risks / Trade-offs

- **Quote must exist beforehand** → confirmation does not create quotes, so callers/tests must provide one. Accepted: quote creation needs the pricing engine, a later change.
- **Service depends on many repositories** → constructor injection stays readable; no god-repository.
- **Relay not implemented** → events are enqueued `PENDING` but not delivered yet. Accepted; the outbox guarantees they are not lost, and a relay is a separate change.
- **Concurrency edge (same key, simultaneous)** → protected by the unique constraint (no duplication); the losing attempt errors for now. Returning the winner's order on that race is a follow-up.

## Open Questions

- Should `confirm(...)` accept a `tenantId`, or derive it from the quote only? Current plan: accept `tenantId` and scope the quote lookup by it.
- Idempotency conflict (same key, different quote) returns which error shape once HTTP lands? Deferred to the endpoint change.
