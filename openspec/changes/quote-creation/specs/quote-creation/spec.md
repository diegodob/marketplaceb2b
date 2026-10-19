# Spec Delta

## Purpose

Turns a priced cart into an immutable, tenant-scoped quote that the order confirmation reads, so the quoted and confirmed totals cannot diverge.

## ADDED Requirements

### Requirement: Create a quote from a cart

The service SHALL expose an endpoint that prices a cart and persists an immutable quote, returning its public identifier, its expiry and the breakdown. The quote SHALL store the ruleset, the customer, the payment method, the total and the expiry, and SHALL never be modified.

#### Scenario: A cart becomes a quote

- **WHEN** a client posts a cart
- **THEN** the quote is persisted with its public identifier and total, and a later price or rule change does not alter it

### Requirement: The quote freezes its lines, discounts and gifts

The quote SHALL persist, per paid line, the unit price, the tax rate, the taxable base and the tax; the applied discounts with the lever that produced them; and each gifted unit set with its product, quantity, taxable flag, value and tax.

#### Scenario: Frozen details

- **WHEN** a quote is created
- **THEN** its lines, applied discounts and gifts are stored and read back unchanged

### Requirement: A quote belongs to a tenant and its customer

A quote SHALL belong to the cart's tenant and SHALL reference a customer of that same tenant.

#### Scenario: A customer from another tenant is rejected

- **WHEN** a quote would reference a customer of another tenant
- **THEN** it is rejected
