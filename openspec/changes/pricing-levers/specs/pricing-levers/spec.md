# Spec Delta

## Purpose

Defines the immutable domain types for the marketplace's commercial levers — scale discounts, combos and gifts — with the validity rules a pricing engine will rely on, without calculating anything yet.

## ADDED Requirements

### Requirement: Scale discount targets a SKU or a family

A scale discount SHALL target either a single product (SKU) or a product family, never both and never neither.

#### Scenario: SKU target carries a product and no family

- **WHEN** a scale discount is created with the SKU target type
- **THEN** it carries a product identifier and no family, and a validator reports it as valid

#### Scenario: Inconsistent target is reported

- **WHEN** a scale discount's target type and its product/family fields do not match
- **THEN** a validator reports the inconsistency

### Requirement: Scale discount has cumulative tiers

A scale discount SHALL have at least one tier, and each tier SHALL have a positive starting quantity and a percentage between `0` and `100`.

#### Scenario: Empty tiers are reported

- **WHEN** a scale discount has no tiers
- **THEN** a validator reports it

#### Scenario: Non-positive starting quantity is reported

- **WHEN** a tier's starting quantity is zero or negative
- **THEN** a validator reports it

#### Scenario: Out-of-range percentage is reported

- **WHEN** a tier's percentage is below `0` or above `100`
- **THEN** a validator reports it

### Requirement: Combo is a set of items with a price

A combo SHALL list at least one item (a product and a positive minimum quantity) and a non-negative combo price.

#### Scenario: Empty items are reported

- **WHEN** a combo has no items
- **THEN** a validator reports it

#### Scenario: Non-positive minimum quantity is reported

- **WHEN** a combo item's minimum quantity is zero or negative
- **THEN** a validator reports it

#### Scenario: Negative combo price is reported

- **WHEN** a combo's price is negative
- **THEN** a validator reports it

### Requirement: Gift has a condition and a quantity

A gift SHALL have a purchase condition and a positive quantity of the gifted product, and the condition SHALL be consistent with its type.

#### Scenario: Non-positive gift quantity is reported

- **WHEN** a gift's quantity is zero or negative
- **THEN** a validator reports it

#### Scenario: Inconsistent condition is reported

- **WHEN** a gift condition's type and its product/family fields do not match
- **THEN** a validator reports the inconsistency

### Requirement: Levers are versioned by ruleset

Every lever SHALL carry the ruleset version it belongs to.

#### Scenario: Each lever carries a ruleset

- **WHEN** a lever is inspected
- **THEN** it exposes a non-null ruleset identifier

### Requirement: Lever money and percentages use BigDecimal

Lever monetary amounts and percentages SHALL be represented with `java.math.BigDecimal`.

#### Scenario: Combo price and tier percentage are BigDecimal

- **WHEN** a combo price or a tier percentage is inspected
- **THEN** its type is `BigDecimal`
