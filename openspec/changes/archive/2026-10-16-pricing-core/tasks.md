# Tasks

## 1. DTOs and reads

- [x] 1.1 Add the cart and breakdown DTOs (`Cart`, `CartLine`, `PriceBreakdown`, `LineBreakdown`, `DiscountBreakdown`, `DiscountOrigin`, `GiftBreakdown`); verify the project compiles
- [x] 1.2 Add a read for the tenant's current ruleset (greatest `effective_from` not in the future) and reads for the ruleset's product prices (unit price + tax category) and tax rates; verify a test resolves the seeded ruleset, prices and rates

## 2. Engine

- [x] 2.1 Implement `OrderPricingService.price(cart)`: gross, taxable base, tax and total, with per-line rounding to the ruleset scale/mode and the breakdown DTO populated; verify a plain cart prices correctly (gross + tax = total, no discounts)
- [x] 2.2 Fail explicitly when the tenant has no ruleset, a product has no price, or a category has no rate; verify with tests

## 3. Build verification

- [x] 3.1 Run `gradlew.bat clean test` and confirm the full suite passes with exit code `0`

## Workflow follow-up

- After the maintainer merges the PR, archive the change (`openspec archive pricing-core`).
