# Proposal

## Why

The baseline engine (`order-pricing`, from `pricing-core`) computes gross, tax and total but applies no commercial levers. The take-home's levers can coexist on the same order, so to add them in a way that is predictable and cannot double-discount, the engine needs a **pricing pipeline**: each lever is a strategy, run in a fixed, configurable priority order, and each consumes the units it discounts. This change adds that pipeline and the first lever, the **combo**.

## What Changes

- Add the **pricing pipeline**: a `LeverStrategy` interface and a `PricingPipeline` that runs the strategies in a configured priority order; each strategy consumes the units it discounts, so a later strategy never discounts the same units again.
- Add the **combo strategy**: a combo applies when all of its items reach their minimum quantity; among the applicable combos the engine picks one by a fixed, documented priority (never by minimizing the total); the combo covers the minimum quantities at its total price, units above the minimum are charged at the regular price, the saving is reported as a **combo discount entry with its origin**, and the combo's units are excluded from any later step.
- **Out of scope:** the scale and gift strategies, quote creation and the credit-fits check (each its own slice).

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `order-pricing`: adds the pricing pipeline and the combo step.

## Impact

- New service classes (the pipeline and the strategy contract) and a pricing context threaded through the strategies; `OrderPricingService.price(cart)` runs the pipeline before computing tax.
