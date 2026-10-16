# Design

## Context

The lever domain types (`ScaleDiscount`/`ScaleTier`, `Combo`/`ComboItem`, `Gift`/`GiftCondition`) and the `product_family` entity exist in `model`. The schema (`V1`–`V3`) does not store levers yet. See proposal.md for motivation and `specs/data-model/spec.md` for the behaviour contract. This change is the persistence for the levers, not the engine.

## Goals / Non-Goals

**Goals:**
- Tables, repositories and seed for the levers, tied to a `ruleset`.
- Constraints that mirror the domain invariants that can be expressed in SQL.

**Non-Goals:**
- The pricing engine and the order of application.
- Enforcing tier ordering in the database (it stays app-validated).
- Outbox relay, HTTP.

## Decisions

**Additive migration `V4__levers.sql`.** `V1`–`V3` are released, so the levers are added through a new migration. Every lever row carries `ruleset_id`, so a lever belongs to a commercial-rules version.

**Scale target is one of `product_id`/`family_id`, consistent with `target_type`.** A `CHECK` enforces `(target_type = 'SKU' AND product_id IS NOT NULL AND family_id IS NULL) OR (target_type = 'FAMILY' AND family_id IS NOT NULL AND product_id IS NULL)` — mirroring the domain's `isTargetConsistent`.

**Tier ranges in the database; ordering in the app.** `scale_tier` gets `CHECK (from_qty > 0)` and `CHECK (percentage BETWEEN 0 AND 100)`. The strictly-increasing rule is **not** a database constraint (it would need a per-group check); it stays the model's `@AssertTrue`, validated when the levers are loaded.

**Combo price and items.** `combo.combo_price >= 0`; `combo_item.min_qty > 0` with `UNIQUE (combo_id, product_id)` so a product appears once per combo.

**Gift condition is its own table.** `gift_condition` holds the optional target (`product_id`/`family_id`, never both), the optional `min_qty`/`min_amount`, and `CHECK`s for "at least one threshold", "a quantity threshold names exactly one target", and the value ranges. `gift` references a condition and the gifted product/quantity.

**Repositories load aggregates.** `ScaleDiscountRepository`, `ComboRepository` and `GiftRepository` read the parent rows (`SELECT ... WHERE ruleset_id = :rulesetId`) and their children (tiers/items/condition), assembling the domain records, over the existing `PersistenceAccess` + `RowMapper`.

**Seed in `V5__levers_seed.sql`.** Adds a product family, sets family assignments on the seeded products, and inserts the example levers (a SKU scale, a family scale, a combo and gifts for the four condition shapes).

**Explicit foreign-key indexes.** Every FK column on the new tables gets a named index.

## Risks / Trade-offs

- **Tier ordering is only app-validated** → a bad order could be stored if data is inserted outside the application. Accepted by team decision; the loader validates.
- **No uniqueness per target** → two scales for the same `(ruleset, product)` are currently allowed and would be ambiguous. Flagged in Open Questions.
- **Aggregate loading is N+1** → fine for the small rule sets here; a join can replace it if needed.

## Open Questions

- Should a target have **at most one** scale per ruleset (`UNIQUE (ruleset_id, product_id)` and `UNIQUE (ruleset_id, family_id)`)?
- Is a `gift_condition` always used by exactly one gift, or shared across gifts?
