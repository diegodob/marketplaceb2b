# Tasks

Depends on `lever-priority` (each lever carries a `priority`). Assumptions from design.md: a priority-driven pipeline with exclusivity via consumed units; the taxable base computed from the reduced line values.

## 1. Pricing pipeline

- [ ] 1.1 Add the `LeverStrategy` interface and a `PricingContext` (cart, ruleset, prices, rates, mutable discounts/gifts and per-product line value + consumed units); verify the project compiles
- [ ] 1.2 Add `PricingPipeline` (collects the ruleset's applicable levers, orders them by descending `priority`, ties by id, and applies each via its type's strategy) and run it in `OrderPricingService.price(cart)` before computing tax; verify a cart with no applicable lever prices exactly as today
- [ ] 1.3 Extend `DiscountBreakdown` with the source lever id (`origin`, `sourceId`, `detail`, `amount`) so a discount can be traced to the lever that produced it, and `LineBreakdown` with `discount` (the line keeps its list gross; its taxable base is `gross − discount`); verify the project compiles

## 2. Combo strategy

- [ ] 2.1 Implement the combo strategy: applicable when every item's still-available units reach its minimum; covers exactly `minQty` per item by recording, per covered line, its discount share (by list value, the last line taking the rounding remainder) while the line keeps its list gross; charges the extras at the regular price; consumes the covered units; records a combo discount entry with the lever's type and id; verify with tests
- [ ] 2.2 Compute each line's taxable base as its list gross minus its discount and the tax on that base; verify the combo lowers the base and the tax

## 3. Golden tests and build

- [ ] 3.1 Add the take-home's combo golden tests: the products that activate the combo do not appear as a loose discount; a combo with taxes (the tax enters the total)
- [ ] 3.2 Run `gradlew.bat clean test` and confirm the full suite passes with exit code `0`

## Workflow follow-up

- After the maintainer merges the PR, archive the change (`openspec archive pricing-combo`).
