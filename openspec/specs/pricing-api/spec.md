# pricing-api Specification

## Purpose

Exposes the order-pricing engine over HTTP so a client can price a cart and receive the per-line and total breakdown, with explicit, non-leaking errors for carts the engine cannot price.

## Requirements

### Requirement: Price a cart over HTTP

The service SHALL expose an HTTP endpoint that takes a cart for a tenant and returns the priced breakdown: gross value, discount, taxable base, tax rate, tax and total to pay, per line and in totals, the discount entries and, when the payment method uses credit, the credit fit.

#### Scenario: A valid cart is priced

- **WHEN** a client posts a cart with a tenant and its lines
- **THEN** the response carries the per-line breakdown and the totals computed by the engine

#### Scenario: A credit cart reports the fit

- **WHEN** the cart's payment method uses credit
- **THEN** the response carries the credit fit

### Requirement: A product that cannot be sold is a client error

A cart line that references a product with no price in the tenant's current pricing SHALL be rejected with a **422** client error, carrying a stable code and a clear message, and SHALL never be priced.

#### Scenario: An unpriced product is rejected

- **WHEN** a cart line references a product that has no price in the tenant's pricing
- **THEN** the endpoint responds 422 with a stable code and a clear message, and no breakdown

### Requirement: Unavailable or misconfigured pricing is a server-side failure

When the tenant has no pricing in force the endpoint SHALL answer **503**, and when a product's tax category has no rate it SHALL answer **500**; both with a stable code and no breakdown, never as a client error.

#### Scenario: A tenant without pricing is unavailable

- **WHEN** a cart's tenant has no pricing in force
- **THEN** the endpoint responds 503 with a stable code and no breakdown

#### Scenario: A missing tax rate is a server-side failure

- **WHEN** a cart product's tax category has no rate in the tenant's pricing
- **THEN** the endpoint responds 500 with a stable code and no breakdown

### Requirement: An invalid cart is rejected

The endpoint SHALL reject a cart that carries no tenant, no lines, or a line with a non-positive quantity, with a **400** client error, and SHALL not price it.

#### Scenario: A line with a non-positive quantity is rejected

- **WHEN** a cart carries a line with a quantity of zero or less
- **THEN** the endpoint responds 400 with a stable code and does not return a breakdown

#### Scenario: A cart without lines is rejected

- **WHEN** a cart carries no lines
- **THEN** the endpoint responds 400 with a stable code and does not return a breakdown

### Requirement: Error bodies do not expose internals

Every error response SHALL carry a stable, machine-readable code and a clear message, and SHALL NOT include internal identifiers, database or exception text, or stack traces.

#### Scenario: An error body carries no internals

- **WHEN** the endpoint answers with any error
- **THEN** the body carries a stable code and a clear message and no internal identifiers or exception text

### Requirement: A payment method that cannot be resolved is a client error

A cart payment method id that does not resolve in the cart's tenant SHALL be rejected with a **422** client error, carrying a stable code and a clear message, and SHALL never be priced.

#### Scenario: An unknown payment method is rejected

- **WHEN** a cart references a payment method that does not exist in its tenant
- **THEN** the endpoint responds 422 with a stable code and a clear message, and no breakdown
