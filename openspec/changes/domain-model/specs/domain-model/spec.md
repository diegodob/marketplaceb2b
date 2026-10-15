# Spec Delta

## Purpose

Defines the immutable Java types that represent the marketplace order domain. They carry their structural constraints as Bean Validation annotations and store monetary values exactly as provided; rounding to a currency scale belongs to the layer that computes the value, not to the model.

## ADDED Requirements

### Requirement: Money and rates use BigDecimal

Domain monetary amounts and tax rates SHALL be represented with `java.math.BigDecimal`. No domain type SHALL use `float` or `double` for money or rates.

#### Scenario: Monetary and rate fields are BigDecimal

- **WHEN** a domain type that carries a monetary amount or a rate is inspected
- **THEN** the field's type is `BigDecimal`

### Requirement: The model does not round monetary values

A domain type SHALL store monetary and rate values exactly as provided and SHALL NOT round or rescale them. Rounding to the currency scale is the responsibility of the layer that computes the value.

#### Scenario: Values are stored as provided

- **WHEN** a quote line is created with a taxable base of `40000.004` and a tax of `8400.0008`
- **THEN** the stored values are exactly `40000.004` and `8400.0008`, unchanged by the model

#### Scenario: A rate is carried at its own scale

- **WHEN** a tax rate of `10.5000` is carried
- **THEN** it keeps `10.5000`

### Requirement: Structural constraints are declared declaratively

Domain types SHALL declare their structural constraints with `jakarta.validation` annotations rather than failing at construction.

#### Scenario: Required components are non-null

- **WHEN** the domain types are inspected
- **THEN** required components carry `@NotNull`

#### Scenario: Quantity is positive

- **WHEN** the quote-line type is inspected
- **THEN** its quantity is annotated `@Positive`

#### Scenario: Tax rate is bounded

- **WHEN** the tax-rate type is inspected
- **THEN** its rate is constrained to `0`–`100` (`@DecimalMin`/`@DecimalMax`)

#### Scenario: Unit price is non-negative

- **WHEN** the product-price type is inspected
- **THEN** its unit price is annotated `@PositiveOrZero`

#### Scenario: Credit balance is within the limit

- **WHEN** the credit-account type is inspected
- **THEN** it declares a class-level check that `available` is within `0..credit_limit`

#### Scenario: A validator reports the violations

- **WHEN** a `Validator` runs on an instance that breaks the declared constraints
- **THEN** the violations are reported

### Requirement: Construction does not enforce constraints

Creating a domain instance SHALL NOT throw on a constraint violation. A violation SHALL be surfaced only when a validator runs; enforcement at the application boundary is deferred to the layer that invokes validation.

#### Scenario: Construction succeeds despite a violation

- **WHEN** a quote line with quantity `0` is constructed
- **THEN** no error is thrown at construction, and the violation is reported only when a validator runs

### Requirement: Supported rounding modes only

A rounding mode SHALL be limited to the supported set, `HALF_UP` and `HALF_EVEN`.

#### Scenario: Rounding mode is a closed set

- **WHEN** the rounding-mode type is inspected
- **THEN** it offers exactly `HALF_UP` and `HALF_EVEN` and no other value

### Requirement: Immutable domain types

Domain types SHALL be immutable: once constructed, a value cannot change, and the types expose no mutating operations.

#### Scenario: No mutators exposed

- **WHEN** a domain type's public API is inspected
- **THEN** it exposes no setter or other in-place mutation
