# pricing-levers Specification

## Purpose
Defines the immutable domain types for the marketplace's commercial levers — scale discounts, combos and gifts — plus the product-family entity they reference, with the validity rules a pricing engine will rely on, without calculating anything yet.

## Requirements

### Requirement: Scale discount targets a SKU or a family

A scale discount SHALL target either a single product (SKU) or a product family, never both and never neither. The family is a `ProductFamily` entity, not free text.

#### Scenario: SKU target carries a product and no family

- **WHEN** a scale discount is created with the SKU target type
- **THEN** it carries a product identifier and no family reference, and a validator reports it as valid

#### Scenario: Inconsistent target is reported

- **WHEN** a scale discount's target type and its product/family fields do not match
- **THEN** a validator reports the inconsistency

### Requirement: Scale discount has cumulative, increasing tiers

A scale discount SHALL have at least one tier, each tier SHALL have a positive starting quantity and a percentage between `0` and `100`, and the tiers SHALL be strictly increasing by starting quantity.

#### Scenario: Empty tiers are reported

- **WHEN** a scale discount has no tiers
- **THEN** a validator reports it

#### Scenario: Non-positive starting quantity is reported

- **WHEN** a tier's starting quantity is zero or negative
- **THEN** a validator reports it

#### Scenario: Out-of-range percentage is reported

- **WHEN** a tier's percentage is below `0` or above `100`
- **THEN** a validator reports it

#### Scenario: Non-increasing tiers are reported

- **WHEN** a tier's starting quantity is not greater than the previous tier's
- **THEN** a validator reports it

### Requirement: Combo is a set of items with a total price

A combo SHALL list at least one item (a product and a positive minimum quantity) and a non-negative price that is the total for the set.

#### Scenario: Empty items are reported

- **WHEN** a combo has no items
- **THEN** a validator reports it

#### Scenario: Non-positive minimum quantity is reported

- **WHEN** a combo item's minimum quantity is zero or negative
- **THEN** a validator reports it

#### Scenario: Negative combo price is reported

- **WHEN** a combo's price is negative
- **THEN** a validator reports it

### Requirement: Gift condition and quantity

A gift SHALL have a purchase condition and a positive quantity of the gifted product. The condition SHALL require at least one threshold: a quantity of a product or family and/or a minimum amount. When both a quantity and an amount threshold are present they SHALL combine as AND. A quantity threshold SHALL name a product or a family (never both), and a minimum amount SHALL not be negative.

#### Scenario: Condition without a threshold is reported

- **WHEN** a gift condition specifies neither a quantity nor an amount
- **THEN** a validator reports it

#### Scenario: Quantity threshold without a target is reported

- **WHEN** a gift condition specifies a quantity but names no product or family
- **THEN** a validator reports it

#### Scenario: Both targets are reported

- **WHEN** a gift condition names both a product and a family
- **THEN** a validator reports it

#### Scenario: Amount and quantity thresholds are both allowed

- **WHEN** a gift condition specifies a positive quantity of a target and a non-negative minimum amount
- **THEN** a validator reports it as valid

#### Scenario: Non-positive gift quantity is reported

- **WHEN** a gift's quantity is zero or negative
- **THEN** a validator reports it

### Requirement: Products belong to a tenant-scoped family

A `ProductFamily` SHALL belong to a tenant, and a product SHALL reference its family by a family identifier.

#### Scenario: Family is tenant-scoped

- **WHEN** a product family is inspected
- **THEN** it carries a tenant identifier

#### Scenario: Product references its family

- **WHEN** a product with a family is inspected
- **THEN** it carries a family identifier rather than a free-text family name

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
