# Proposal

## Why

The order-flow rules (exact money, positive quantities, bounded credit, valid tax rates, closed sets such as ledger entry types and outbox destinations) currently live only in the schema spec. There are no Java types to carry them across the service layer. An immutable, validated domain model gives the upcoming persistence and service layers a single, test-checked vocabulary and makes those invariants expressible in code, not only in the database.

## What Changes

- Add the `model` layer (`com.marketplaceb2b.model`) with **immutable Java records** for the domain entities (including the persistence-only rows `IdempotencyKey` and `OutboxEvent`) and **enums** for the closed sets (`RoundingMode`, `LedgerEntryType`, `OutboxDestination`, `OutboxEventType`, `OutboxStatus`).
- Declare structural constraints with **Bean Validation** annotations (`jakarta.validation`) instead of failing in the constructor; the validation implementation is added now (as a test dependency) so tests can run a `Validator`, while enforcement at the application boundary lands with the service layer in a later change.
- Keep the model a **plain holder**: it stores monetary values exactly as provided and does **not** round or rescale them. Rounding to the currency scale is the responsibility of the layer that computes the value (the pricing engine, in a later change).
- Represent money and rates with `BigDecimal` (never `float`/`double`).
- Add unit tests covering validator-driven constraint detection and that values are carried unchanged.
- **Out of scope:** Flyway SQL, JDBC and the data-access layer; services; HTTP endpoints; anything already owned by `persistence-layer`.

## Capabilities

### New Capabilities
- `domain-model`: the immutable, validated Java types (entities and enums) that represent the marketplace order domain and reject invalid values at construction.

### Modified Capabilities
- None.

## Impact

- New source: `src/main/java/com/marketplaceb2b/model/**` and matching tests under `src/test/java/com/marketplaceb2b/model/**`.
- New dependencies: `jakarta.validation:jakarta.validation-api` (compile) and the Bean Validation implementation (`spring-boot-starter-validation`) as a **test** dependency so tests can run a `Validator`; the implementation moves to the main classpath when the services enforce validation.
- No existing spec or behavior is modified.
- The types are persistence-agnostic and will be consumed by `persistence-layer` and `domain-services`.
