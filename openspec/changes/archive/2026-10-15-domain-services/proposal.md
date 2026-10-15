# Proposal

## Why

The schema can store quotes and orders, but nothing orchestrates the confirmation flow — the step that turns a quote into an order idempotently, consumes credit and emits outbound events. Without it, the take-home's core invariant (a `201` means the order exists and is queryable, and retries never duplicate it) cannot be exercised end to end.

## What Changes

- Add **repositories** (data-access classes) for the reads and writes confirmation needs: quotes, payment methods, credit accounts, idempotency keys, sales orders, credit ledger and outbox.
- Add an **`OrderConfirmationService`** that confirms a quote in a **single transaction**, using only the repositories — **no SQL in the service**.
- Extend `PersistenceAccess` with an insert that returns the generated id.
- Add integration tests for the confirmation behaviours.
- **Out of scope:** the pricing engine and quote creation, catalog/customer/credit CRUD, outbox relay delivery, and HTTP endpoints.

## Capabilities

### New Capabilities
- `order-confirmation`: idempotent, atomic confirmation of a quote into an order, with credit consumption and outbound events, orchestrated through repositories.

### Modified Capabilities
- None.

## Impact

- New packages: `com.marketplaceb2b.service` (the service) and `com.marketplaceb2b.persistence.repository` (the repositories).
- Consumes the existing `model` types and the Flyway schema; adds no migrations.
- No existing spec or behaviour is modified.
