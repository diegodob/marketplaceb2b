# order-pricing Specification

## Purpose
Prices a cart against a tenant's active commercial rules into a per-line and total breakdown with exact money, as the base the lever, quote and credit slices build on.

## Requirements

### Requirement: Order breakdown

Given a cart and the tenant's current ruleset, the engine SHALL return a breakdown per line and in totals: gross value, the taxable base, the tax and the total to pay, plus the discount and gift entries (empty until the lever slices add them).

#### Scenario: Breakdown reports per-line and totals

- **WHEN** a cart is priced against a valid ruleset
- **THEN** each line carries its product, quantity, unit price, gross value, tax rate, taxable base and tax, and the result carries the gross subtotal, the total taxable base, the total tax and the total

#### Scenario: No discounts yet

- **WHEN** a cart is priced by this baseline
- **THEN** the discount and gift entries are empty and the total equals the gross plus the tax

### Requirement: Current ruleset resolution

The engine SHALL price against the tenant's current ruleset — the row with the greatest `effective_from` that is not in the future — and SHALL fail with an explicit error when the tenant has none.

#### Scenario: Missing ruleset is an error

- **WHEN** a cart is priced for a tenant that has no ruleset in force
- **THEN** the calculation fails with an explicit error

### Requirement: Price and tax rate are required

A cart product without a price in the ruleset SHALL fail, and a product whose tax category has no rate in the ruleset SHALL fail, both with an explicit error (never a price or rate of zero).

#### Scenario: Product without a price is an error

- **WHEN** a cart line references a product that has no price in the ruleset
- **THEN** the calculation fails with an explicit error

#### Scenario: Category without a rate is an error

- **WHEN** a product's tax category has no rate in the ruleset
- **THEN** the calculation fails with an explicit error

### Requirement: Exact money and per-line rounding

Monetary values SHALL use `BigDecimal` with no floating point. Each line's taxable base and tax SHALL be rounded to the ruleset currency scale using the ruleset rounding mode, and the total SHALL be the sum of the rounded values.

#### Scenario: Rounded per line, summed for the total

- **WHEN** a cart is priced
- **THEN** every line amount has the ruleset currency scale and the total equals the sum of the rounded line values
