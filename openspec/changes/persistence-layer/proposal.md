# Proposal

## Why

The service skeleton boots but persists nothing. The take-home order flow (catalog → cart → quote → confirmation → history) needs a relational schema whose constraints make the hard invariants unbreakable: quoted and confirmed totals cannot diverge, money is exact, prices/taxes/rules are versioned and per-tenant, confirmation is idempotent, and outbound effects are never lost. This change defines and materializes that base data model.

## What Changes

- Add **H2 in PostgreSQL-compatible mode** and **Flyway** to the build, and configure the datasource and migration location.
- Add Flyway migrations (`V1`…) creating the base schema: `country`, `tenant`, `tax_category`, `ruleset`, `tax_rate`, `product`, `product_price`, `payment_method`, `customer`, `credit_account`, `quote`, `quote_line`, `sales_order`, `idempotency_key`, `credit_ledger`, `outbox`.
- Encode the domain invariants as database constraints: surrogate `id` primary keys (`BIGINT GENERATED ALWAYS AS IDENTITY`), explicit `UNIQUE`/`CHECK` constraints, non-guessable public identifiers (`quote.public_id`, `sales_order.public_number`), and explicit indexes on every foreign key.
- Money as `DECIMAL(19,4)` and tax rates as `DECIMAL(7,4)` (never floating point); per-line monetary results rounded before storage; currency, decimal scale and rounding mode sourced from `ruleset`.
- Immutable quote → order path: `quote`/`quote_line` are insert-only and store frozen `unit_price`/`tax_rate`; `sales_order` is unique per quote and changes only via `deleted_at` (soft delete); the order amount is read from its quote, never recalculated.
- Append-only `credit_ledger` with signed movements; `idempotency_key` guarding confirmation retries; a transactional `outbox` for external effects.
- Per-tenant isolation by a `tenant_id` column on shared tables, verified by a two-tenant isolation test.
- **Out of scope:** scales, combos, gifts and any discounts; the pricing/quote/confirmation/outbox-relay logic and their HTTP endpoints; the ADR. The schema SHALL allow those to be added later via new migrations without altering existing tables.

## Capabilities

### New Capabilities
- `data-model`: the relational schema — tables, keys, `UNIQUE`/`CHECK` constraints, foreign-key indexes and the domain invariants each constraint protects — that persists the multi-tenant marketplace order flow.

### Modified Capabilities
- None.

## Impact

- Build: add H2 (PostgreSQL mode) and Flyway dependencies; configure the datasource and `db/migration` location.
- New source: `src/main/resources/db/migration/` (Flyway SQL), plus persistence access code and integration tests (implementation phase).
- New spec: `openspec/specs/data-model/spec.md`. No existing spec or behavior is modified.
