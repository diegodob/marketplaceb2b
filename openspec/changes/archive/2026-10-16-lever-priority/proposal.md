# Proposal

## Why

The levers need an explicit, data-driven precedence: the distributor should decide which applies first, instead of the engine inferring it (for example, from the combo that covers the most units). This change gives every lever a `priority`; the pricing engine orders the levers by it in a later change.

## What Changes

- Add `priority` (`INT NOT NULL DEFAULT 0`) to `scale_discount`, `combo` and `gift`, via migration `V7__lever_priority.sql` (plus a small backfill of example priorities).
- Add a `priority` component to the `ScaleDiscount`, `Combo` and `Gift` records and map it in their repositories.
- Semantics: **higher priority is applied first**; ties are broken deterministically by id.
- **Out of scope:** the engine's use of the priority — that lands in `pricing-combo`, `pricing-scale` and `pricing-gift`.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `data-model`: the lever tables (`scale_discount`, `combo`, `gift`) gain a `priority` column.

## Impact

- New migration `V7`; the lever records, their repositories, the seed and the model tests.
