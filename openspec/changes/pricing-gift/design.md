# Design

## Context

See proposal.md — Why. The pipeline and the combo and scale strategies exist (`service.pricing`). A gift is a `Gift` — a `GiftCondition` (an optional product or family plus optional quantity and amount thresholds), the gift product, its quantity, a `taxable` flag and a priority — loaded by `GiftRepository.findByRuleset`. The **gift product may not be in the cart**, so its price and rate are not among the prices the service loads for the cart lines. `LineBreakdown` carries gross, discount, taxable base, rate and tax; `DiscountOrigin` has `COMBO` and `SCALE`.

## Goals / Non-Goals

**Goals:**
- Apply the gift as part of the same pipeline, with the gift visible where the buyer expects it: as a product line.
- Keep the "no product paid" rule while the taxable gift's tax is charged.
- Never let the free units trigger a combo or a scale.

**Non-Goals:**
- Quote creation and the credit-fits check.

## Decisions

**The gift is a marked line plus a GIFT discount.** When a condition is met, the strategy adds a `PricingLine` for the gift product (its quantity and unit price, marked as a gift) and a `GIFT` discount equal to the line's value, so the line's net base is zero and the buyer pays no product. Alternative: a separate `gifts`-only entry — rejected by the product owner: the gift should read as a product line with its quantity and value, and the GIFT discount makes it free.

**The gift tax ignores the GIFT discount.** For a **taxable** gift the line's tax is computed on its **gross** (value × rate), not on the zero net base; for a **non-taxable** gift the tax is zero. This is a documented exception to "tax on the base after discounts", justified by the taxable event on a free good. Alternative: tax on the net base — rejected: it would always be zero and lose the take-home's tax implication.

**Isolation via running last and marking.** The gift runs after the discounts (its priority, seeded low) and every gift line carries a gift mark; the combo and scale strategies **skip** marked lines, so the free units never reach them — and a gift configured with a high priority still cannot trigger them.

**Condition on the gross cart.** The quantity and amount thresholds read the cart's **original** units and list value (not the post-lever remaining units or the discounted value); gift lines are excluded from those totals. Alternative: evaluate on available units / discounted value — rejected: a combo or scale would silently cancel a gift.

**Accumulation.** Every met condition grants its gift (product owner's decision).

**The response keeps a `gifts` summary** derived from the gift lines (`productId`, `qty`, `taxable`, `tax`), so a client can read the gifted quantity without filtering the lines. The line's gift mark is the source of truth.

**Gift product price and rate loaded separately.** The service loads the gift products' prices/rates (they may not be in the cart). A taxable gift requires them; if either is missing the engine fails with the existing explicit errors.

**Persistence (documented for quote-creation).** A quote stores the gift as a line; whether that is a `gift` mark on `quote_line` or a `quote_gift` entry is a quote-creation decision. `unit_price` on a gift line is the product's list price, reduced to zero by the GIFT discount — a deliberate, documented representation.

## Example: two products plus a taxable gift

Cart: `GAS-500` ×12 (4000, 12%) and `LEC-1L` ×4 (6000, 0%). The seeded SKU scale `12 → 3%` applies to GAS. The gift condition `GAS-500 ≥ 12` grants `AGU-500` ×1, taxable.

| Line | qty | unit | gross | discount | base | rate | tax |
|---|---|---|---|---|---|---|---|
| GAS-500 | 12 | 4000 | 48000.00 | 1440.00 | 46560.00 | 12% | 5587.20 |
| LEC-1L | 4 | 6000 | 24000.00 | 0.00 | 24000.00 | 0% | 0.00 |
| AGU-500 (gift) | 1 | 2500 | 2500.00 | 2500.00 (GIFT) | 0.00 | 12% | **300.00** |

Discounts: `SCALE` 1440.00 and `GIFT` 2500.00. Gift summary: `AGU-500` ×1, taxable, tax 300.00.

| grossSubtotal | totalDiscounts | totalTaxableBase | totalTax | total |
|---|---|---|---|---|
| 74500.00 | 3940.00 | 70560.00 | 5887.20 | 76447.20 |

The gift's tax is on its **gross** (2500 × 12% = 300) although its net base is zero; the GIFT discount makes the gift free but does not erase its tax.

## Risks / Trade-offs

- **The tax exception** means a gift line's `tax ≠ base × rate`. Documented and asserted; the gift mark explains it.
- **The gross subtotal includes the gift value** and the total discounts include the GIFT amount, so both look larger than the paid amounts. Transparent: the buyer sees the gift and its 100% reduction.
- **A high-priority gift** would still be isolated by the gift mark (the other strategies skip it).
- **The gift line is stored as a line** in a quote; the flag/table shape is a quote-creation decision.

## Open Questions

- None.
