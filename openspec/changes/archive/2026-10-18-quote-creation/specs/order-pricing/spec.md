# Spec Delta

## MODIFIED Requirements

### Requirement: Order breakdown

Given a cart and the tenant's current ruleset, the engine SHALL return a breakdown per line and in totals: gross value, discount, the taxable base, the tax and the total to pay, plus the discount entries, each identifying the line it applies to. A gift appears as a marked line, never as a separate list.

#### Scenario: Breakdown reports per-line and totals

- **WHEN** a cart is priced against a valid ruleset
- **THEN** each line carries its product, quantity, unit price, gross value, discount, tax rate, taxable base and tax, and the result carries the gross subtotal, the total discounts, the total taxable base, the total tax and the total

#### Scenario: No discounts yet

- **WHEN** a cart is priced and no lever applies
- **THEN** the discount entries are empty and the total equals the gross plus the tax

#### Scenario: A discount entry identifies its line

- **WHEN** a lever discounts one or more lines
- **THEN** each discount entry names the product it applies to
