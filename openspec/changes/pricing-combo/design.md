# Design

## Context

`pricing-core` computes gross, tax and total with no levers. The lever tables and their repositories exist. This change adds the engine's lever **pipeline** and the **combo** strategy. See proposal.md and `specs/order-pricing/spec.md`.

## Goals / Non-Goals

**Goals:**
- A strategy-based, exclusive, priority-ordered pipeline so levers can be added without touching the engine.
- The combo step, predictable and documented (never minimizing the total).

**Non-Goals:**
- The scale and gift strategies, quote creation, the credit-fits check.
- A per-lever database priority (see Open Questions).

## Decisions

**Pipeline over procedural steps.** A `LeverStrategy` interface (`apply(PricingContext)`) and a `PricingPipeline` that holds an **ordered list** of strategies and runs them in that order. Adding a lever is adding a strategy; the precedence is the list order (configuration), not scattered code. Alternative: procedural steps in the service — rejected: rigid and hard to reorder or test in isolation.

**Exclusivity via consumed units.** `PricingContext` tracks, per product, the line value and the units already discounted (`consumedQty`). A strategy only discounts the **remaining** units and marks what it consumed. This makes "the combo excludes the scale" automatic, without either strategy knowing the other.

**PricingContext.** Carries the cart, the ruleset (currency scale and rounding mode), the loaded prices/rates, and the mutable accumulators `discounts` (list of `DiscountBreakdown`), `gifts` (list of `GiftBreakdown`) and the per-product line value + consumed units. `OrderPricingService.price(cart)` builds it, runs the pipeline, then computes the taxable base from the (reduced) line values and the tax on top.

**Combo strategy.** For the ruleset's combos, an **applicable** combo is one whose every item's cart quantity reaches its minimum. Among the applicable combos the engine picks one by a fixed, documented rule — the combo covering the **most units**, ties broken by combo id — never by minimizing the total. The chosen combo covers, per item, `min(minQty, remaining units)` at the combo price: the covered units' list value is replaced by the combo price, and the **combo discount entry** carries `(covered list value − combo price)` with its origin. Units above the minimum keep the regular price; the covered units are marked consumed so a later step skips them.

**Priority source.** Cross-lever precedence is the pipeline's ordered strategy list (combo → scale → gift), configured where the pipeline is built. A per-lever `priority` column in the database is deferred (Open Questions).

## Risks / Trade-offs

- **Prorating the combo price across its items** → the covered units' value is set to the combo price, distributed across items by their list-value share, so each line keeps a base. Documented; reused by the tax step.
- **Combos never minimize the total** → the buyer may not get the theoretical best price, but the result is predictable and auditable (the business priority).
- **Pipeline adds indirection** → small; each strategy is independently testable, and the engine keeps its current tests.

## Open Questions

- Add a per-lever `priority` column (schema) so precedence is data-driven per tenant, or keep the pipeline order as configuration?
