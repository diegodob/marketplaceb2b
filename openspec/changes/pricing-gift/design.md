# Design

## Context

See proposal.md — Why. The pipeline and the combo and scale strategies exist (`service.pricing`). A gift is a `Gift` — a `GiftCondition` (an optional product or family plus optional quantity and amount thresholds), the gift product, its quantity, a `taxable` flag and a priority — loaded by `GiftRepository.findByRuleset`. The **gift product may not be in the cart**, so its price and rate are not among the prices the service loads for the cart lines. `GiftBreakdown(productId, qty, taxable, tax)` already exists.

## Goals / Non-Goals

**Goals:**
- Add the gift as another `LeverStrategy`, with no special path in the pipeline.
- Evaluate the condition on the gross cart and accumulate every met gift.
- Account for a taxable gift's tax in the totals.

**Non-Goals:**
- Quote creation and the credit-fits check.

## Decisions

**Condition on the gross cart.** The quantity and amount thresholds read the cart's **original** units and list value (not the post-lever remaining units or the discounted value), matching the README. The gift rewards what the buyer bought, not what the discounts left. Alternative: evaluate on available units / discounted value — rejected: a combo or scale would silently cancel a gift.

**Accumulation.** Every met condition grants its gift (product owner's decision); the breakdown lists each. Alternative: only the largest gift — rejected.

**Gift product price and rate loaded separately.** The service loads the gift products' prices/rates (they may not be in the cart). A **taxable** gift requires its product's price and rate; if either is missing the engine fails with the existing explicit errors (never a 0 tax). A non-taxable gift needs neither.

**The gift tax enters the totals.** The tax on the gift's list value (unit price × gifted quantity, at the product's rate) is added to `totalTax` and `total` and carried in `GiftBreakdown.tax`. It is not a paid line, so it is not in the line bases: the total can exceed the sum of the line taxes by the gift tax.

**Gifts neither consume nor trigger.** The gifted units are additions, not cart units: they do not consume the cart's units and are not seen by the combo or the scale (which run on the cart lines). The strategy records the gift entry and its tax; it does not touch the lines.

**Gift entries, not lines.** The free units are reported in the `gifts` list with their quantity, not as a zero-price line (the take-home's breakdown).

## Risks / Trade-offs

- **A taxable gift's tax has no paid base** → the total exceeds the sum of the line taxes by the gift tax. Documented and asserted.
- **The gross-cart condition** can grant a gift even when the discounts reduce the paid amount below the threshold. Intended.
- **Loading the gift products' prices** adds queries; bounded by the small rule set.

## Open Questions

- None.
