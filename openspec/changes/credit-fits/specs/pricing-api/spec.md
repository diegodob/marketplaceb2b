# Spec Delta

## MODIFIED Requirements

### Requirement: Price a cart over HTTP

The service SHALL expose an HTTP endpoint that takes a cart for a tenant and returns the priced breakdown: gross value, discount, taxable base, tax rate, tax and total to pay, per line and in totals, the discount entries and, when the payment method uses credit, the credit fit.

#### Scenario: A valid cart is priced

- **WHEN** a client posts a cart with a tenant and its lines
- **THEN** the response carries the per-line breakdown and the totals computed by the engine

#### Scenario: A credit cart reports the fit

- **WHEN** the cart's payment method uses credit
- **THEN** the response carries the credit fit

## ADDED Requirements

### Requirement: A payment method that cannot be resolved is a client error

A cart payment method id that does not resolve in the cart's tenant SHALL be rejected with a **422** client error, carrying a stable code and a clear message, and SHALL never be priced.

#### Scenario: An unknown payment method is rejected

- **WHEN** a cart references a payment method that does not exist in its tenant
- **THEN** the endpoint responds 422 with a stable code and a clear message, and no breakdown
