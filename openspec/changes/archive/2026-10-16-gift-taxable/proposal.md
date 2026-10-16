# Proposal

## Why

The pricing engine must know whether a gift's free units are taxed. Today the gift lever carries its condition and quantity but no signal for its tax treatment, so it cannot decide if the gifted value enters the taxable base.

## What Changes

- Add a **`taxable` flag** to the gift:
  - `boolean taxable` on the `Gift` domain type;
  - `taxable BOOLEAN NOT NULL DEFAULT FALSE` on the `gift` table, added by migration `V6__gift_taxable.sql` (existing gifts stay non-taxable).
- Map the flag in `GiftRepository` and update the model tests for the new component.
- **Out of scope:** using the flag in a calculation — the pricing engine reads it in a later change.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `data-model`: `gift` gains a `taxable` column.

## Impact

- `Gift` (domain), `GiftRepository`, migration `V6`, and the gift model tests.
- No behaviour change until the pricing engine reads the flag.
