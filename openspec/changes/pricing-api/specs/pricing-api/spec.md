# Spec Delta

## Purpose

Exposes the order-pricing engine over HTTP so a client can price a cart and receive the per-line and total breakdown, with explicit client errors for carts the engine cannot price.

## ADDED Requirements

### Requirement: Price a cart over HTTP

The service SHALL expose an HTTP endpoint that takes a cart for a tenant and returns the priced breakdown: gross value, discount, taxable base, tax rate, tax and total to pay, per line and in totals, plus the discount and gift entries.

#### Scenario: A valid cart is priced

- **WHEN** a client posts a cart with a tenant and its lines
- **THEN** the response carries the per-line breakdown and the totals computed by the engine

### Requirement: A cart that cannot be priced is a client error

When the engine cannot price the cart — the tenant has no ruleset in force, a product has no price in that ruleset, or a product's tax category has no rate — the endpoint SHALL answer with a client error carrying a machine-readable error body, never a successful breakdown.

#### Scenario: An unpriced product is a client error

- **WHEN** a cart line references a product that has no price in the tenant's ruleset
- **THEN** the endpoint responds with a client error and an error body, not a breakdown

#### Scenario: A tenant without a ruleset is a client error

- **WHEN** a cart's tenant has no ruleset in force
- **THEN** the endpoint responds with a client error and an error body

### Requirement: An invalid cart is rejected

The endpoint SHALL reject a cart that carries no tenant, no lines, or a line with a non-positive quantity, with a client error, and SHALL not price it.

#### Scenario: A line with a non-positive quantity is rejected

- **WHEN** a cart carries a line with a quantity of zero or less
- **THEN** the endpoint responds with a client error and does not return a breakdown

#### Scenario: A cart without lines is rejected

- **WHEN** a cart carries no lines
- **THEN** the endpoint responds with a client error and does not return a breakdown
