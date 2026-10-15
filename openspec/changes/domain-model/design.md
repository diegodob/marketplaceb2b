# Design

## Context

`api-foundation` defines a layered layout with a `model` layer that is still empty. This change fills it. See proposal.md for motivation and `specs/domain-model/spec.md` for the behavior contract. The types must be persistence-agnostic so `persistence-layer` can map them to rows with JDBC and `domain-services` can operate on them without either layer dictating the other.

## Goals / Non-Goals

**Goals:**
- One immutable Java type per domain entity, plus enums for the closed sets.
- Structural constraints expressed declaratively and detected by a `Validator` (exercised in tests now).
- A model with no dependency on pricing context: it holds values, it does not compute or round them.
- Fast plain-JUnit tests (no Spring context).

**Non-Goals:**
- Rounding or rescaling monetary values (that is the pricing engine's responsibility, in a later change).
- Enforcing validation at the application boundary (that lands with `domain-services`).
- Persistence (SQL, JDBC, mapping) and services.

## Decisions

**Immutable records, not classes.** Each entity is a Java `record`, giving immutability, value equality and no setters for free. Alternative: classes with getters/setters or Lombok — rejected: mutability and an unnecessary dependency.

**Declarative validation via Bean Validation, not constructor throwing.** Structural constraints are declared with `jakarta.validation` annotations on the components (`@NotNull`, `@Positive`, `@DecimalMin`/`@DecimalMax`, `@PositiveOrZero`) and, for the cross-field rule, a class-level `@AssertTrue` method (`CreditAccount.isAvailableWithinLimit()`). Constructors do not throw on violations; a violation is surfaced only when a `Validator` runs. Enforcement at the boundary is deferred to `domain-services`. Alternative: validating in compact constructors — rejected by the team as too rigid and as spreading validation logic across the model. Dependencies: `jakarta.validation:jakarta.validation-api` on the main classpath, and the implementation (`spring-boot-starter-validation`) as a **test** dependency so tests can run a `Validator`; the implementation moves to the main classpath when the services enforce validation.

**The model is a plain holder; rounding is the engine's job.** `Quote` and `QuoteLine` are ordinary records: they store the monetary values they are given and neither round nor rescale them. The pricing engine, which already holds the `ruleset` and produces the numbers, is responsible for rounding each line to the currency scale before the value reaches the model. This keeps `model` free of any dependency on `Ruleset` and avoids a second rounding site. Alternative considered: factory methods on `Quote`/`QuoteLine` that take a `Ruleset` and round on construction — rejected because it duplicated a responsibility that already lives in the engine and coupled the model to pricing context.

**`BigDecimal` for money and rates; enums for closed sets.** `RoundingMode` (`HALF_UP`, `HALF_EVEN`), `LedgerEntryType`, `OutboxDestination`, `OutboxEventType` and `OutboxStatus` are enums, so invalid literals are impossible. `Ruleset` carries `currencyScale` and a `RoundingMode` as data; it is not used by the model to compute anything.

**`Long id`, nullable until persisted.** Records use `Long` for identity so a not-yet-persisted instance is representable; the persistence layer assigns the real `id` on insert.

**Package `com.marketplaceb2b.model`.** Entities and enums in that package; it fills the `model` layer promised by `api-foundation`.

**Type set.** Entities: `Country`, `Tenant`, `TaxCategory`, `Ruleset`, `TaxRate`, `Product`, `ProductPrice`, `PaymentMethod`, `Customer`, `CreditAccount`, `Quote`, `QuoteLine`, `SalesOrder`, `IdempotencyKey`, `CreditLedgerEntry`, `OutboxEvent`. Persistence-only rows (`IdempotencyKey`, `OutboxEvent`) are included here so every entity has one home.

## Risks / Trade-offs

- **Annotation constraints are metadata, not enforcement** → nothing stops an invalid instance from existing until a validator runs. Accepted: tests exercise a `Validator` now, and enforcement at the boundary is a deliberate later step in `domain-services`; the database remains the source of truth for persistence.
- **The model does not guarantee rounding** → a caller could hand it an unrounded amount. Accepted: the only producer of these values is the pricing engine, which owns rounding (with its own tests); the model stays free of pricing context.
- **Validation implementation on the test classpath only** → production enforcement is not active until the services add the implementation. Accepted; annotated on purpose so the risk is visible.
- **Nullable components (`id`, `family`, `deletedAt`, `nextAttemptAt`, `lastError`, `sentAt`)** → left unannotated; `@NotNull` covers only the required ones.

## Open Questions

- Should the cross-field rule move from `@AssertTrue` to a custom class-level constraint when the pricing change needs scale/element checks too?
