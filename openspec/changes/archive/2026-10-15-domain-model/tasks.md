# Tasks

Assumptions carried from design.md: Java records, `BigDecimal` for money/rates, declarative `jakarta.validation` constraints (enforced at the boundary only in `domain-services`), the model stores values as provided and does **not** round (rounding is the pricing engine's job), `Long id` (nullable until persisted), package `com.marketplaceb2b.model`.

## 1. Dependencies and enums

- [x] 1.1 Add `jakarta.validation:jakarta.validation-api` (main classpath) and `org.springframework.boot:spring-boot-starter-validation` (test scope) to `build.gradle.kts`; verify a test can build a `Validator` from `Validation.buildDefaultValidatorFactory()` and that `gradlew.bat -q dependencies --configuration compileClasspath` resolves
- [x] 1.2 Create `RoundingMode` (`HALF_UP`, `HALF_EVEN`), `LedgerEntryType` (`CONSUMPTION`, `REVERSAL`), `OutboxDestination` (`PUSH`, `ERP`), `OutboxEventType` (`ORDER_CONFIRMED`, `ORDER_CANCELLED`) and `OutboxStatus` (`PENDING`, `SENT`, `FAILED`); verify a test asserts `RoundingMode` exposes exactly the two supported values

## 2. Money and rate types (plain holders)

- [x] 2.1 Create `Quote` and `QuoteLine` as plain records with `@NotNull` on required components and `@Positive` on `qty`; verify a test asserts the stored monetary values equal the inputs exactly (no rounding or rescaling by the model)
- [x] 2.2 Create `ProductPrice` (`@PositiveOrZero` unit price) and `TaxRate` (`@DecimalMin`/`@DecimalMax` `0`–`100`); verify a test asserts `4000.0005` is returned unchanged and a rate of `10.5000` keeps its scale

## 3. Declarative constraints

- [x] 3.1 Create `Country`, `Tenant`, `TaxCategory`, `Customer`, `Product`, `PaymentMethod`, `Ruleset`, `SalesOrder`, `IdempotencyKey` and `OutboxEvent` with `@NotNull` on required components and nullable ones unannotated; verify an annotation-presence test passes
- [x] 3.2 Create `CreditAccount` declaring the class-level rule that `available` is within `0..credit_limit` (a `@AssertTrue` method); verify an annotation-presence test passes
- [x] 3.3 Add a test that runs a `Validator` and detects the declared violations (quantity `0`, rate outside `0`–`100`, `available` outside `0..credit_limit`, a null required component); verify it passes
- [x] 3.4 Verify construction does not enforce constraints: constructing a `QuoteLine` with quantity `0` throws nothing

## 4. Architecture and build

- [x] 4.1 Add an architecture test asserting every `model` type is a `record` with no setters and that all monetary/rate components are `BigDecimal` (never `float`/`double`); verify it passes
- [x] 4.2 Run `gradlew.bat clean test` on a clean checkout and confirm the full suite passes with exit code `0`

## Workflow follow-up

- After the maintainer merges the PR, archive the change (`openspec archive domain-model`).
