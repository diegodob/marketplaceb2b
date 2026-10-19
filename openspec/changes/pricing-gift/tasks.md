# Tasks

## 1. Model and context

- [x] 1.1 Add `GIFT` to `DiscountOrigin`, a gift mark to `LineBreakdown`, and the gift mark plus taxability to `PricingLine`; let `PricingContext` accept added lines; make the combo and scale strategies skip gift lines
- [x] 1.2 Load the ruleset's gifts and the gift products' prices/rates in `OrderPricingService` and carry them in `PricingContext`; verify the project compiles

## 2. Gift strategy

- [x] 2.1 Implement `GiftStrategy`: applicable when the condition is met on the gross cart (the quantity of the product/family and/or the gross value, combined with AND, excluding gift lines); for every met condition add a marked gift line (quantity and unit price) and a `GIFT` discount equal to its value

## 3. Tax and totals

- [x] 3.1 In the breakdown, compute a taxable gift line's tax on its gross (the documented exception) and add it to the total tax and the total; a non-taxable gift line pays no tax; remove the separate `gifts` list (`GiftBreakdown` and `PriceBreakdown.gifts`)

## 4. Tests

- [x] 4.1 Happy path: a quantity condition, an amount condition, both (AND), a taxable gift (its tax on the gross enters the total) and a non-taxable gift
- [x] 4.2 Edge cases: no condition met; several conditions accumulate; a gift line does not reach the scale/combo; the gift is a marked line with a GIFT discount; the cart already carries the gift product (two lines, the gift one marked); different tax rates; the CO currency scale; another tenant's gift does not apply
- [x] 4.3 Run `gradlew.bat clean test` and confirm the full suite passes with exit code `0`

## 5. Documentation

- [x] 5.1 Document the gift model in the README (line + GIFT discount, tax on the gross for taxable gifts, isolation, gross-cart condition) and update the scope; leave the persistence of a repeated gift product as an open question for quote-creation

## Workflow follow-up

- After the maintainer merges the PR, archive the change (`openspec archive pricing-gift`).
