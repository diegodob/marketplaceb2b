# Design

## Context

The lever tables (`scale_discount`, `combo`, `gift`) and their domain records exist. Their precedence is not represented anywhere. See proposal.md and `specs/data-model/spec.md`.

## Goals / Non-Goals

**Goals:**
- A `priority` on every lever so precedence is data-driven.
- Storage, domain and repository plumbing for it.

**Non-Goals:**
- The engine's ordering (a later change).

## Decisions

**`priority` on every lever.** `scale_discount`, `combo` and `gift` gain `priority INT NOT NULL DEFAULT 0`. Higher is applied first; ties are broken by `id`. This replaces inferring the order (for example, from a combo's size).

**Additive migration.** `V1`–`V6` are released, so `V7__lever_priority.sql` adds the column with `DEFAULT 0`, then backfills example priorities (combos above scales) so the seed exercises the order.

**Domain and mapping.** `ScaleDiscount`, `Combo` and `Gift` gain an `int priority` component; `ScaleDiscountRepository`, `ComboRepository` and `GiftRepository` map it. Adding a component breaks the record constructions in the model and repository tests; those are updated with the new component.

## Risks / Trade-offs

- **Default 0 for existing rows** → without the backfill they would all tie; the migration sets examples and real data would set meaningful values.
- **Touching the lever records and their tests** → mechanical updates for the new component.

## Open Questions

- None.
