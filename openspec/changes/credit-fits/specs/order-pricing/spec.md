# Spec Delta

## ADDED Requirements

### Requirement: Credit fit

When the cart's payment method uses credit, the breakdown SHALL carry the credit fit: whether the total fits the available credit, the available amount, the total and a reason (`FITS`, `EXCEEDS_LIMIT`, or `NO_ACCOUNT` when the customer has no credit account). The fit SHALL be null when the cart has no payment method or its method does not use credit.

#### Scenario: A credit order within the available credit

- **WHEN** the total does not exceed the customer's available credit
- **THEN** the fit reports `FITS` with the available amount and the total

#### Scenario: A credit order above the available credit

- **WHEN** the total exceeds the customer's available credit
- **THEN** the fit reports `EXCEEDS_LIMIT` and does not fit

#### Scenario: A credit customer without an account

- **WHEN** the payment method uses credit and the customer has no credit account
- **THEN** the fit reports `NO_ACCOUNT` and does not fit

#### Scenario: No credit check

- **WHEN** the cart has no payment method or its method does not use credit
- **THEN** the fit is null

### Requirement: Payment method resolution

The engine SHALL resolve the cart's payment method scoped by the cart's tenant, and SHALL fail with an explicit error when the id does not resolve in that tenant.

#### Scenario: A payment method from another tenant does not resolve

- **WHEN** a cart references a payment method that belongs to another tenant
- **THEN** the engine fails with an explicit error
