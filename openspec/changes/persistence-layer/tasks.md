# Tasks

Assumptions carried from design.md: H2 in PostgreSQL compatibility mode, Flyway-owned schema, JDBC (`JdbcClient`) access, quote validity read from an application property. Deferred (see design.md Open Questions): whether the chosen H2 version supports SQL-bodied immutability triggers.

## 1. Persistence dependencies and configuration

- [x] 1.1 Add H2 and Flyway dependencies to `build.gradle.kts`; verify resolution with `gradlew.bat -q dependencies --configuration runtimeClasspath`
- [x] 1.2 Configure the H2 datasource (in-memory, `MODE=PostgreSQL`) and Flyway `db/migration`, and add the quote-validity property; verify the app starts and Flyway logs the migrations as applied
- [x] 1.3 Add Spring JDBC (`JdbcClient`) and verify a context-loading test obtains a working `JdbcClient`/`DataSource` connection

## 2. V1 reference schema

- [x] 2.1 Author `V1__schema.sql` with the reference group (`country`, `fiscal_id_type`, `tenant`, `tax_category`, `ruleset`, `tax_rate`), identity PKs, `UNIQUE`/`CHECK` constraints and explicit FK indexes; verify the migration applies cleanly
- [x] 2.2 Add integration tests asserting rejection of: duplicate `country.code`; duplicate `(country_id, code)` in `fiscal_id_type`; duplicate `tenant.name`; duplicate `(tenant_id, version)` and duplicate `(tenant_id, effective_from)` in `ruleset`; out-of-range and duplicate `(ruleset_id, tax_category_id)` in `tax_rate`; a FK to an unknown parent; verify all tests pass

## 3. V2 catalog and credit

- [x] 3.1 Add the catalog and credit group (`product`, `product_price`, `payment_method`, `customer`, `credit_account`) to `V1__schema.sql` with constraints and indexes; verify the migration applies cleanly
- [x] 3.2 Add integration tests asserting rejection of: duplicate SKU within a tenant; duplicate `(ruleset_id, product_id)` and negative `unit_price`; duplicate `(tenant_id, code)` and missing `uses_credit`; a customer with a missing `fiscal_id` or `fiscal_id_type_id` and a customer with an unknown `fiscal_id_type_id`; a second credit account for one customer; `available < 0` and `available > credit_limit`; verify all tests pass
- [x] 3.3 Add the multi-tenant isolation test with two tenants holding the same SKU, asserting a tenant-scoped read returns only that tenant's rows

## 4. V3 quotes

- [x] 4.1 Add the quotes group (`quote`, `quote_line`) to `V1__schema.sql` with constraints and indexes; verify the migration applies cleanly
- [x] 4.2 Add integration tests asserting rejection of: duplicate `quote.public_id`; duplicate `(quote_id, product_id)`; non-positive `qty`; verify all tests pass

## 5. V4 orders, idempotency, ledger, outbox

- [x] 5.1 Add the orders/idempotency/ledger/outbox group (`sales_order`, `idempotency_key`, `credit_ledger`, `outbox`) to `V1__schema.sql` with constraints and indexes; verify the migration applies cleanly
- [x] 5.2 Add integration tests asserting rejection of: a second order for the same `quote_id`; duplicate `sales_order.public_number`; duplicate `(customer_id, idem_key)`; two `CONSUMPTION` rows for one order; duplicate `(sales_order_id, destination, event_type)`; invalid enum values in `credit_ledger.entry_type`, `outbox.destination`, `outbox.event_type` and `outbox.status`; verify all tests pass

## 6. Immutability and append-only enforcement

- [x] 6.1 Enforce no in-place modification of `ruleset`, `quote`, `quote_line` and `credit_ledger` via `BEFORE UPDATE`/`BEFORE DELETE` triggers, or an access-layer guard if the H2 version cannot express them; verify a direct `UPDATE`/`DELETE` is refused (and record the chosen mechanism in design.md Open Questions)
- [x] 6.2 Add a test proving `credit_ledger` exposes no modification path and that the ledger invariant `credit_limit + sum(amount) = available` holds

## 7. Seed data and acceptance

- [x] 7.1 Add the seed as a Flyway migration `V2__seed.sql` (country AR; `fiscal_id_type` `CUIT` for AR; tenant; ruleset `AR-2026-10-01.1` with `currency_scale` 2 and `rounding_mode` `HALF_UP`; `GENERAL`/`REDUCED` categories; three products with prices; `CREDIT`/`CASH` methods; customer 12 with fiscal id and limit 500000.00 and available 300000.00); verify it applies and is idempotent across restarts
- [x] 7.2 Add an acceptance test reproducing the example: quote total `93070.00` equals the sum of `taxable_base + tax` over its lines; a money-scale test asserts no stored monetary value exceeds its `currency_scale`; `sales_order` has no total column; a confirmed order yields two `PENDING` outbox rows (`PUSH`, `ERP`, `ORDER_CONFIRMED`); a replayed confirmation returns the same order

## 8. Integration verification

- [x] 8.1 Add a test asserting every foreign key has an explicit index (via JDBC metadata / `information_schema`)
- [x] 8.2 Run `gradlew.bat clean test` on a clean checkout and confirm the full suite (constraints, immutability, isolation, money, acceptance) passes with exit code `0`

## Workflow follow-up

- After the maintainer merges the PR, archive the change (`openspec archive persistence-layer`).
