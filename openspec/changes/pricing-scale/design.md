# Design

## Context

See proposal.md — Why. The lever pipeline exists (`service.pricing`: `LeverStrategy`, `PricingContext`, `PricingLine`, `PricingPipeline`, `ComboStrategy`) and the context already carries the ruleset's combos. A scale is a `ScaleDiscount` (target `SKU`/`FAMILY`, a `productId` or `familyId`, strictly increasing tiers, a priority), loaded by `ScaleDiscountRepository.findByRuleset`. The `product` table carries `family_id`, but `ProductPrice` does not, and there is no product repository yet; `data-model` requires every product query to be scoped by tenant and proved by a multi-tenant test.

## Goals / Non-Goals

**Goals:**
- Add the scale as another `LeverStrategy`, with no special path in the pipeline.
- Resolve a family scale's products through a tenant-scoped lookup.
- Report the saving as an entry with its origin and consume the discounted units.

**Non-Goals:**
- Progressive brackets (a different model, rejected — see Decisions).
- The gift lever, quote creation and the credit-fits check.

## Decisions

**Reached tier on all units.** Once the target's units reach a tier, that tier's percentage applies to **all** of the target's units (a whole-line percentage per threshold). Example: tiers `12 → 3%` and `24 → 7%`, 30 units bought → all 30 at 7%. Alternative: progressive brackets (12–23 at 3%, 24–30 at 7%) — rejected by the product owner: the distributor's scale is a single percentage per threshold.

**The scale is a `LeverStrategy`.** `ScaleDiscountStrategy` reports its applicable scales and applies one by id, exactly like the combo strategy; the pipeline orders it by priority and the consumed units give exclusivity.

**Product → family resolution with a tenant-scoped lookup.** A new `ProductRepository.findByIds(tenantId, productIds)` returns the cart's products with their `family_id`. Alternative: query products by `family_id` alone — rejected: it is not tenant-scoped, and `data-model`'s multi-tenant isolation requirement demands a tenant filter and a test. A family scale sums the available units of the cart lines whose product belongs to the family.

**Reach and consume on available units.** The target's reach is computed on the units a higher-priority lever left (cart quantity minus consumed); a scale consumes the target's available units it discounts, so a lower-priority scale on the same target sees the remainder (with the reached-tier model, generally none).

**Discount math per line.** For each covered line, the discount is its covered list value times the reached percentage (`Money.percentage`), rounded per line to the currency scale; the line's discount accumulates and the entry's amount is the sum of the line discounts. Alternative: apply the percentage to the family aggregate and prorate — more code for the same per-line rounding the rest of the engine uses.

## Risks / Trade-offs

- **Per-line rounding** → a family scale's summed discounts can differ by a cent from applying the percentage to the aggregate. Mitigated: documented; consistent with the engine's per-line rounding.
- **A product without a family** can never match a FAMILY scale. Expected.
- **A second scale on the same target is effectively unreachable** (the first consumes all its units). Intended: priority + exclusivity, never "the greater discount".
- **The README's old scale note** says "the one giving the greater discount is used"; it is corrected to the priority rule in this change.

## Open Questions

- None.
