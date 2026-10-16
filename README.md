# marketplaceb2b-api

Multi-tenant B2B marketplace API for the order flow: catalog → cart → checkout (quote) → confirmation → history.

A Java 21 / Spring Boot WebFlux service. This repository contains the order-flow foundation: an immutable domain model, a Flyway-managed schema (H2 in PostgreSQL compatibility mode), a JDBC data-access layer with repositories, and an idempotent order-confirmation service. The pricing engine, outbox relay and HTTP endpoints are still pending (see [Scope](#scope)).

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
├── service                        # application logic (order confirmation)
├── model                          # immutable domain types (records + enums)
└── persistence                    # data access
    ├── PersistenceAccess          # JDBC wrapper + append-only guard
    └── repository                 # one repository per aggregate
```

Schema lives in `src/main/resources/db/migration` (`V1__schema.sql`, `V2__seed.sql`, `V3__product_family.sql`).

## Scope

Implemented:

- Bootable Java 21 Spring Boot WebFlux service; `GET /api/v1/health`
- Immutable, validated domain model (records + Bean Validation): countries, tenants, tax categories, rulesets, tax rates, product families, products, prices, payment methods, customers, credit accounts, quotes/lines, orders, idempotency keys, credit ledger, outbox, and the commercial levers (scale discounts, combos, gifts)
- Flyway schema: `V1__schema.sql` (tables), `V2__seed.sql` (example data), `V3__product_family.sql`
- JDBC data-access layer (`PersistenceAccess` + repositories) with an append-only guard
- Idempotent, atomic order confirmation (`OrderConfirmationService`): validates the quote, consumes credit when the method uses it, and writes the order, idempotency key, ledger entry and outbox events in one transaction

Pending:

- **Pricing engine** (apply the levers) and the lever tables (`scale_discount`, `scale_tier`, `combo`, `combo_item`, `gift`, `gift_condition`)
- **Outbox relay** (delivery of pending events)
- **HTTP endpoints** (quote, `POST /orders`)
- **ADR**
- Optional Angular/Flutter front end

## Design notes (pending)

The take-home requires the sections below. They will be completed as the corresponding
capabilities land; nothing here should be read as a committed policy.

### Lever application order

TBD — not yet defined.

### Rounding policy

TBD — not yet defined.

### Consistency model

TBD — not yet defined.

### Known simplifications & rules (for the ADR)

- **Scale discounts target a single product or a single family.** A scale that should span several targets is modelled as several scale discounts. Tiers are cumulative and strictly increasing by starting quantity.
- **Combo minimums are a floor.** Each combo item requires a minimum quantity; units **above** the minimum are charged at the regular price (e.g. a minimum of 6 with 8 bought: the 6 go in the combo, the extra 2 are paid).
- **Gift thresholds combine with AND.** A gift condition may require a quantity of a product/family, a minimum amount, or both; when both are present the order must meet both. A disjunction is expressed as two separate gift conditions.
