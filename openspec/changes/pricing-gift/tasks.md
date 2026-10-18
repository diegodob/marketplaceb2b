# Tasks

## 1. Load the gifts

- [ ] 1.1 Load the ruleset's gifts and the gift products' prices/rates in `OrderPricingService`, carry them in `PricingContext`, and add the gift taxes to the breakdown's total tax and total; verify the project compiles

## 2. Gift strategy

- [ ] 2.1 Implement `GiftStrategy`: applicable when the condition is met on the gross cart (quantity of the product/family and/or the gross value, combined with AND); every met condition records a `GiftBreakdown` with its quantity, `taxable` flag and tax; it does not consume units or touch the lines

## 3. Tests

- [ ] 3.1 Happy path: a quantity condition, an amount condition, both (AND), a taxable gift (its tax enters the total) and a non-taxable gift
- [ ] 3.2 Edge cases: no condition met; several conditions accumulate; a family condition sums its products; a gift does not change the scale/combo; different tax rates; the CO currency scale; another tenant's gift does not apply
- [ ] 3.3 Run `gradlew.bat clean test` and confirm the full suite passes with exit code `0`

## 4. Documentation

- [ ] 4.1 Document the gift rule in the README (gross cart, accumulation, gift tax) and update the scope

## Workflow follow-up

- After the maintainer merges the PR, archive the change (`openspec archive pricing-gift`).
