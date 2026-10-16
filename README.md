# marketplaceb2b-api

Multi-tenant B2B marketplace API for the order flow: catalog → cart → checkout (quote) → confirmation → history.

A Java 21 / Spring Boot WebFlux service. This repository contains the order-flow foundation: an immutable domain model, a Flyway-managed schema (H2 in PostgreSQL compatibility mode), a JDBC data-access layer with repositories, an order-pricing engine baseline, and an idempotent order-confirmation service. The lever application, quote creation, outbox relay and HTTP endpoints are still pending (see [Scope](#scope)).

## Stack

- **Java 21** (Gradle toolchain)
- **Spring Boot 3.5.x** with **WebFlux** (reactive, Netty)
- **Gradle** (Kotlin DSL) via the committed Gradle wrapper
- **H2** in-memory in **PostgreSQL compatibility mode**; **Flyway** for migrations
- **JDBC** (Spring `JdbcClient`) with repositories; no JPA
- JUnit 5 + `WebTestClient` for tests

## Requirements

- A JDK 21 toolchain available to Gradle. The build requests Java 21 and fails rather than silently compiling for an older target.

## Commands

Run the service:

```
# Windows
gradlew.bat bootRun

# macOS / Linux
./gradlew bootRun
```

The service listens on port `8080` (see `src/main/resources/application.yaml`). On start it runs the Flyway migrations against the in-memory database. Health check:

```
GET http://localhost:8080/api/v1/health   ->   {"status":"UP"}
```

Run the tests (single build-and-test command):

```
# Windows
gradlew.bat clean test

# macOS / Linux
./gradlew clean test
```

## Structure

Packages under `com.marketplaceb2b` (source root `src/main/java`):

```
com.marketplaceb2b
├── MarketplaceB2bApiApplication   # Spring Boot entry point
├── controller                     # HTTP endpoints
├── dto                            # request/response data-transfer objects
├── model                          # domain types (records, enums, Money)
├── repository                     # PersistenceAccess + one repository per aggregate
└── service                        # application logic
    └── exception                  # domain exceptions
```

Schema lives in `src/main/resources/db/migration` (`V1__schema.sql`, `V2__seed.sql`, `V3__product_family.sql`, `V4__levers.sql`, `V5__levers_seed.sql`, `V6__gift_taxable.sql`).

## Scope

Implemented:

- Bootable Java 21 Spring Boot WebFlux service; `GET /api/v1/health`
- Immutable, validated domain model (records + Bean Validation): countries, tenants, tax categories, rulesets, tax rates, product families, products, prices, payment methods, customers, credit accounts, quotes/lines, orders, idempotency keys, credit ledger, outbox, and the commercial levers (scale discounts, combos, gifts)
- Flyway schema: `V1__schema.sql` (tables), `V2__seed.sql` (example data), `V3__product_family.sql`, `V4__levers.sql`, `V5__levers_seed.sql`, `V6__gift_taxable.sql`
- JDBC data-access layer (`com.marketplaceb2b.repository`: `PersistenceAccess` + repositories) with an append-only guard
- Idempotent, atomic order confirmation (`OrderConfirmationService`): validates the quote, consumes credit when the method uses it, and writes the order, idempotency key, ledger entry and outbox events in one transaction
- Order-pricing engine **baseline** (`OrderPricingService`): resolves the tenant's current ruleset, reads prices and tax rates, and computes the gross value, taxable base, tax and total with per-line rounding

Pending:

- Pricing engine — **levers**: the strategy pipeline that applies combos, scale discounts and gifts, and the golden tests
- **Quote creation** (persist an immutable quote from a cart)
- **Credit-fits** check (does the order fit the cupo)
- **Outbox relay** (delivery of pending events)
- **HTTP endpoints** (quote, `POST /orders` with an idempotency key)
- **ADR**
- Optional Angular/Flutter front end

## Design notes

### Lever application order

The engine applies the commercial levers through a **pipeline of strategies** run in a fixed, configured order: **combo → scale → gift → tax**. Each strategy **consumes** the units it discounts, so a later strategy never discounts the same units again — this prevents double-discounting (a combo's units are excluded from the scale).

- **Combo**: applies only when **all** of its items reach their minimum quantity. Among the applicable combos the engine picks one by a **fixed, documented priority** (never by minimizing the total). The minimum units go to the combo's total price and units **above** the minimum keep the regular price. The saving is reported as a combo entry with its origin.
- **Scale**: targets a single product or a single family; the percentage of the reached tier applies to all of that target's units. When two scales could apply, the one giving the greater discount is used.
- **Gift**: evaluated on the gross cart (before discounts); gifted units do not trigger other levers and are taxed only when the gift is marked `taxable`.
- **Tax**: computed on the taxable base **after** the commercial discounts.

### Rounding policy

Money uses `BigDecimal` (never floating point); monetary amounts are `DECIMAL(19,4)` and rates `DECIMAL(7,4)`. The currency, its decimal scale and the rounding mode come from the `ruleset`. Each line's **taxable base and tax are rounded per line** to the currency scale using the ruleset's rounding mode, and the total is the sum of the already-rounded line values. The arithmetic is encapsulated in a `Money` value object so the rounding cannot be forgotten. A test verifies that stored amounts do not carry more decimals than the currency scale.

### Consistency model

The **quote is immutable** and freezes the ruleset, the prices, the rates and the total, so the quoted and confirmed totals cannot diverge even if prices or rules change between the two moments. Confirmation runs in **a single transaction** (order + idempotency key + credit movement + outbox events) and returns the order number only after the commit; a retry with the same idempotency key returns the original order and never duplicates it or consumes credit twice. Outbound effects use a **transactional outbox** written in the same transaction; delivery is at-least-once and the destination discards duplicates by the event id. The trade-off: at-least-once delivery (no strict exactly-once) and no ordering guarantee.

### Known simplifications & rules (for the ADR)

- **Scale discounts target a single product or a single family.** A scale that should span several targets is modelled as several scale discounts. Tiers are cumulative and strictly increasing by starting quantity.
- **Combos are chosen by priority, not by size or price.** A real system would let the distributor set explicit priorities; the engine never minimizes the total for the buyer. Tiers are applications of a fixed order.
- **Combo minimums are a floor.** Each combo item requires a minimum quantity; units **above** the minimum are charged at the regular price (e.g. a minimum of 6 with 8 bought: the 6 go in the combo, the extra 2 are paid).
- **Gift thresholds combine with AND.** A gift condition may require a quantity of a product/family, a minimum amount, or both; when both are present the order must meet both. A disjunction is expressed as two separate gift conditions.
