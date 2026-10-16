# Proposal

## Why

The take-home's A1 is the order-calculation engine. This first slice is its baseline: from a cart and the tenant's active rules, resolve the ruleset, read prices and tax rates, and produce the per-line and total breakdown with exact money. Levers, quote creation and the credit check are later slices.

## What Changes

- Add **`OrderPricingService.price(cart)`** returning the breakdown (per line and in totals), with empty `discounts` and `gifts` lists ready for the later slices.
- Resolve the tenant's **current ruleset** (the greatest `effective_from` not in the future; error if none).
- Read the cart products' net prices and their tax category, and each category's rate for the ruleset. A product **without a price** in the ruleset fails; a category **without a rate** in the ruleset fails.
- Exact `BigDecimal` money; each line's taxable base and tax are rounded **per line** to the ruleset currency scale and rounding mode; the total is the sum of the rounded values.
- **Out of scope:** combos, scales and gifts (later slices); quote creation (later); the credit-fits check (a separate change); HTTP.

## Capabilities

### New Capabilities
- `order-pricing`: the baseline order-calculation — breakdown, current-ruleset resolution, exact money and per-line rounding — that the lever, quote and credit slices extend.

### Modified Capabilities
- None.

## Impact

- New service and DTOs in `com.marketplaceb2b.service`; new reads for the current ruleset, product prices and tax rates.
- No change to the confirmation flow or the schema.
