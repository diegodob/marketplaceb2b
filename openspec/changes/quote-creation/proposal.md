# Proposal

## Why

The pricing endpoint returns a breakdown but persists nothing, so the checkout cannot freeze what it quoted: the quoted total would be re-priced at confirmation, and A2 requires the quoted and confirmed totals to be identical. The quote and order must also reference a customer of their own tenant, which the schema does not enforce today.

## What Changes

- Add `POST /api/v1/quotes`: prices the cart with the existing engine and persists an **immutable** quote — the header (public id, customer, payment method, ruleset, total, expiry) and the frozen quote lines (unit price, tax rate, taxable base, tax), the applied discounts per line (`quote_line_discount`, B1) and the gifted units (`quote_gift`).
- Enforce tenant-consistent references in the database: `(tenant_id, customer_id)` -> `customer(tenant_id, id)` on `quote` and `sales_order`, and `(tenant_id, quote_id)` -> `quote(tenant_id, id)` on `sales_order`.
- Resolve the gift-persistence open question: a `quote_gift` entry per gift (its gift, product, quantity, taxable flag, value and tax), so a gift does not collide with a paid line of the same product.
- **Out of scope:** the order-confirmation endpoint (A2) and the outbox relay.

## Capabilities

### New Capabilities
- `quote-creation`: the checkout — prices a cart and persists an immutable, tenant-scoped quote (frozen lines, discounts and gifts) that the confirmation reads.

### Modified Capabilities
- `data-model`: adds the tenant-consistent composite foreign keys and the `quote_line_discount` and `quote_gift` tables.

## Impact

- A `V8` migration, a quote writer that maps the breakdown to the quote tables, a `POST /quotes` controller, tests and the README.
