# Tasks

Depends on the pricing engine (the breakdown) and `credit-fits` (the tenant-scoped method/account).

## 1. Engine: a discount entry carries its line

- [ ] 1.1 Add the product a discount applies to on `DiscountBreakdown`, and make the combo and scale emit **one entry per covered line** (its share); the gift entry stays on its gifted line. Update the combo/scale tests for the per-line entries

## 2. Schema

- [ ] 2.1 Add `V8__quote_persistence.sql`: `UNIQUE (tenant_id, id)` on `customer` and `quote`; the composite foreign keys on `quote` and `sales_order` (replacing the single-column customer/quote ones); the `quote_line_discount` and `quote_gift` tables with their indexes; add both new tables to the append-only guard

## 3. Quote writer

- [ ] 3.1 Add a tenant-scoped `CustomerRepository.find(tenantId, id)`; add a `QuoteWriter` that maps the breakdown to `quote`, `quote_line` (the paid lines), `quote_line_discount` (the per-line applied discounts with their lever) and `quote_gift` (the gift lines) in one transaction, rejecting a customer outside the cart's tenant with a `MissingCustomerException`; verify the project compiles

## 4. Endpoint

- [ ] 4.1 Add `POST /api/v1/quotes` returning **201** with the public id, the expiry and the breakdown; map the unknown or cross-tenant customer to a client error; document the endpoint in the README

## 5. Tests — quote creation

- [ ] 5.1 Happy: a cart **without discounts**; a cart with **two discounts** (combo + scale); a gift of a product **already paid** and a gift of a product **not in the cart** — assert the persisted `quote`, `quote_line`, `quote_line_discount` and `quote_gift` rows, and that the stored total equals the breakdown's
- [ ] 5.2 Credit and expiry: a credit method (`FITS` / `INSUFFICIENT_CREDIT` / `NO_ACCOUNT`) and a non-credit method; the expiry is 24h after creation; a later price change does not alter the stored quote
- [ ] 5.3 Errors: a customer of another tenant (the `MissingCustomerException` / the composite FK); an unknown payment method; a cart the engine cannot price (no price, no rate); an invalid cart

## 6. Tests — order confirmation (A2), mirroring the existing ones

- [ ] 6.1 Quote → order: the order's amount is the quote's **stored** price; changing a product price after the quote does not alter the quote or the order
- [ ] 6.2 Two tenants, two quotes: confirm one and not the other (a cross-tenant quote is not found); the order read is tenant-scoped
- [ ] 6.3 With and without credit: credit consumed once; insufficient credit rejected; an expired quote (an expiry in the past) rejected; a replay returns the same order; the same key with another quote is rejected
- [ ] 6.4 A quote produces at most one order

## 7. Cross-cutting

- [ ] 7.1 The composite FK rejects a cross-tenant customer and a cross-tenant quote directly at the database
- [ ] 7.2 Run `gradlew.bat clean test` and confirm the full suite passes with exit code `0`

## Workflow follow-up

- After the maintainer merges the PR, archive the change (`openspec archive quote-creation`).
