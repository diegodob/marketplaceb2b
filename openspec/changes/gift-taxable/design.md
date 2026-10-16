# Design

## Context

The `Gift` domain type (`id`, `rulesetId`, `condition`, `giftProductId`, `giftQty`) and the `gift` table exist (`V4`). Neither carries a tax-treatment signal. See proposal.md for motivation and `specs/data-model/spec.md` for the behaviour contract.

## Goals / Non-Goals

**Goals:**
- A `taxable` flag on the gift, stored and loaded, defaulting to non-taxable.

**Non-Goals:**
- Applying the flag in a calculation (the pricing engine change reads it).

## Decisions

**The flag lives on the gift lever.** `Gift` gains `boolean taxable`; `gift` gains `taxable BOOLEAN NOT NULL DEFAULT FALSE`. The flag is per gift, so two gifts of the same product may differ.

**Additive migration.** `V1`–`V5` are released, so `V6__gift_taxable.sql` adds the column with a `DEFAULT FALSE`, keeping the existing seed gifts non-taxable.

**Domain and mapping.** `GiftRepository` maps `rs.getBoolean("taxable")`. Adding a component to `Gift` breaks its constructions in `PricingLeversTest`; those are updated with the new component.

## Risks / Trade-offs

- **Default false hides the tax path** → the seed's gifts stay non-taxable; the pricing-engine change exercises the taxable path in its tests.
- **Touching `PricingLeversTest`** → mechanical update for the new record component.

## Open Questions

- None.
