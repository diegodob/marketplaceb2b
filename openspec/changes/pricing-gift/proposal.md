# Proposal

## Why

The pipeline applies the combo and the scale, but the gift is not applied: when a purchase condition is met, the buyer should receive free units, and a taxable gift must be taxed on its value.

## What Changes

- Add the **gift strategy**: a gift applies when its condition is met on the **gross cart** (the cart's units and list value, before the other levers); every met condition grants its own gift (they accumulate).
- The gift adds a **line** for the gift product, marked as a gift, and a **GIFT discount** equal to the line's value, so the line's net base is zero and it never pays product. The response keeps no separate gifts list: the marked line and the GIFT discount carry the product, quantity, amount and tax.
- A **GIFT discount does not lower the tax** of a taxable gift: the engine computes that line's tax on its **gross** (the value before the GIFT discount) and adds it to the totals; a non-taxable gift's line pays no tax.
- A gift line neither consumes the cart's units nor makes the combo or the scale apply (the gift runs after them and the other strategies skip gift lines).
- **Out of scope:** quote creation and the credit-fits check.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `order-pricing`: adds the gift step (the gift as a marked line plus a GIFT discount, the tax exception and the isolation from the other levers).

## Impact

- New `GiftStrategy` in `service.pricing`; `DiscountOrigin` gains `GIFT`; `LineBreakdown` carries a gift mark; `PricingLine` a gift mark and its taxability; `PricingContext` accepts added lines and carries the gifts; the breakdown applies the tax exception; the combo and scale strategies skip gift lines.
- Tests for the gift step; the README's gift note and status; the persistence of a gift as a line is documented for quote-creation.
