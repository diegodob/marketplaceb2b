# marketplaceb2b-api

Multi-tenant B2B marketplace API for the order flow: catalog → cart → checkout (quote) → confirmation → history.

This repository currently contains the **service skeleton** only: it boots a reactive HTTP service with a health endpoint. The pricing engine, order confirmation, and ADR described in the take-home are not implemented yet (see [Scope](#scope)).

## Stack

- **Java 21** (Gradle toolchain)
- **Spring Boot 3.5.x** with **WebFlux** (reactive, Netty)
- **Gradle** (Kotlin DSL) via the committed Gradle wrapper
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

The service listens on port `8080` (see `src/main/resources/application.yaml`). Health check:

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
├── controller                     # HTTP endpoints (Spring WebFlux controllers)
├── service                        # application and domain logic (added per capability)
└── model                          # domain types (added per capability)
```

The tree is organized in layers. `service` and `model` are created as their capabilities
land; only `controller` holds code in the current skeleton.

## Scope

Implemented in this skeleton:

- Bootable Java 21 Spring Boot WebFlux service
- `GET /api/v1/health` returning `{"status":"UP"}`
- Layered package layout: `controller` (with `service` and `model` added as capabilities land)

Not implemented yet (out of scope for this change):

- **A1** Order pricing engine (scale discounts, combos, gifts, taxes, credit limit)
- **A2** Idempotent order confirmation and outbound effects
- **A3** ADR
- Optional Angular/Flutter front end

## Design notes (pending)

The take-home requires the sections below. They will be filled in as the corresponding
capabilities are implemented; they are **not yet defined**. Nothing here should be read
as a committed policy.

### Lever application order

TBD — not yet defined.

### Rounding policy

TBD — not yet defined.

### Consistency model

TBD — not yet defined.
