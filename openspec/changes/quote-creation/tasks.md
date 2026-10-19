# Tasks

Depends on the pricing engine (the breakdown) and `credit-fits` (the tenant-scoped method/account).

## 1. Schema

- [ ] 1.1 Add `V8__quote_persistence.sql`: `UNIQUE (tenant_id, id)` on `customer` and `quote`; the composite foreign keys on `quote` and `sales_order` (replacing the single-column customer/quote ones); the `quote_line_discount` and `quote_gift` tables with their indexes; add both new tables to the append-only guard

## 2. Quote writer

- [ ] 2.1 Add a tenant-scoped `CustomerRepository.find(tenantId, id)`; add a `QuoteWriter` that maps the breakdown to `quote`, `quote_line` (the paid lines), `quote_line_discount` (the applied discounts) and `quote_gift` (the gift lines) in one transaction, rejecting a customer that does not belong to the cart's tenant with a `MissingCustomerException`; verify the project compiles

## 3. Endpoint

- [ ] 3.1 Add `POST /api/v1/quotes` returning **201** with the public id, the expiry and the breakdown; map the unknown or cross-tenant customer to a client error; document the endpoint in the README

## 4. Tests

- [ ] 4.1 Happy path: a cart becomes a quote; the lines, discount entries and gift entries are read back unchanged; the stored total equals the breakdown's
- [ ] 4.2 Edge cases: a gift of a product also paid persists in `quote_gift` and not as a second line; a customer from another tenant is rejected; a later price change does not alter the stored quote; the composite FK rejects a cross-tenant reference directly
- [ ] 4.3 Run `gradlew.bat clean test` and confirm the full suite passes with exit code `0`

## Workflow follow-up

- After the maintainer merges the PR, archive the change (`openspec archive quote-creation`).
