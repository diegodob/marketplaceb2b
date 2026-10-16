# Design

## Context

`pricing-core` computes gross, tax and total with no levers. The lever tables, their repositories and a `priority` per lever (`lever-priority`) exist. This change adds the engine's lever **pipeline** and the **combo** strategy. See proposal.md and `specs/order-pricing/spec.md`.

## Goals / Non-Goals

**Goals:**
- Apply the applicable levers in a fixed, explicit order driven by each lever's `priority` (the distributor decides, not an inference), exclusive over consumed units.
- The combo step, predictable and documented (never minimizing the total).

**Non-Goals:**
- The scale and gift strategies' own rules (their changes); quote creation; the credit-fits check.

## Decisions

**Pipeline of strategies, priority-driven.** A `LeverStrategy` per lever type computes that lever's effect on the pricing context. `PricingPipeline` collects the ruleset's **applicable** levers (each type decides what "applicable" means), orders them by **descending `priority`** (ties by `id`), and applies each through its type's strategy. Adding a lever type is adding a strategy. Alternative: type-ordered procedural steps — rejected: rigid and not data-driven.

**Exclusivity via consumed units.** Each applied lever **consumes** the units it discounts (per product). A lower-priority lever only sees the remaining units, so "the combo excludes the scale" is automatic and no lever needs to know the others.

**PricingContext.** Carries the cart, the ruleset (currency scale and rounding mode), the loaded prices/rates, and the mutable accumulators `discounts` (list of `DiscountBreakdown`), `gifts` (list of `GiftBreakdown`) and, per product, the remaining line value and consumed units. The discount entry is a **report** — `origin` (the lever type), the lever's `sourceId` and a label, plus the amount — not a second application: the line values already carry the reduction. `OrderPricingService.price(cart)` builds the context, runs the pipeline, then computes the taxable base from the reduced values and the tax on top.

**Combo strategy.** A combo is **applicable** when every item's **still-available** units reach its minimum. When applied (in priority order), it covers exactly `minQty` units per item at the combo price: the covered units' list value is replaced by the combo price, distributed across the items by their share of that list value (the last item takes the rounding remainder), and the **combo discount entry** carries — with its origin (the lever type and its `sourceId`) — the amount `(covered list value − combo price)`. Units above the minimum keep the regular price; the covered units are consumed. If several combos apply, the higher `priority` runs first and a later combo only sees the units the earlier ones left (so it may no longer be applicable).

## Risks / Trade-offs

- **Prorating the combo price across its items** → the covered units' value is set to the combo price, distributed across items by their list-value share, so each line keeps a base. Documented; reused by the tax step.
- **The engine never minimizes the total** → the buyer may not get the theoretical best price, but the result is predictable and auditable; the distributor controls the order via `priority`.
- **Pipeline adds indirection** → small; each strategy is independently testable, and the engine keeps its current tests.

## Open Questions

- None.
