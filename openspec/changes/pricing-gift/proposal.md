# Proposal

## Why

The pipeline applies the combo and the scale, but the gift — the third commercial lever — is not applied: when a purchase condition is met, the buyer should receive free units, and a taxable gift must be accounted for in the tax.

## What Changes

- Add the **gift strategy**: a gift applies when its condition is met on the **gross cart** (the cart's units and list value, before the other levers); every met condition grants its own gift (they accumulate); a gift adds free units of its product, reported with their quantity, never pays product, never consumes the cart's units and never triggers another lever.
- Account for the **gift tax**: a taxable gift adds the tax on its list value at its product's rate to the total and the total tax; a non-taxable gift adds none.
- **Out of scope:** quote creation and the credit-fits check.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `order-pricing`: adds the gift step (condition on the gross cart, accumulation, gifts do not pay product, gift tax in the total).

## Impact

- New `GiftStrategy` in `service.pricing`; `PricingContext` carries the gifts and the gift products' prices; the breakdown adds the gift taxes to the total and the total tax.
- Tests for the gift step; the README's gift note and status.
