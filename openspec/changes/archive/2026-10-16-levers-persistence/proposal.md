# Proposal

## Why

The commercial levers exist as domain types but nothing persists them. The pricing engine will need to read a tenant's levers for the active ruleset, so the scale discounts, combos and gifts must have their own tables, repositories and seed data — exactly as prices and taxes do.

## What Changes

- Add migration **`V4__levers.sql`** creating the lever tables, all tied to `ruleset_id`:
  - `scale_discount` (target `SKU`/`FAMILY` with exactly one of `product_id`/`family_id`) and `scale_tier` (`from_qty` > 0, `percentage` 0–100).
  - `combo` (`combo_price` ≥ 0) and `combo_item` (`min_qty` > 0, one row per product).
  - `gift_condition` (optional product/family target, optional quantity and/or amount) and `gift` (a gift product with a positive quantity, referencing a condition).
  - Explicit indexes for every foreign key.
- Add migration **`V5__levers_seed.sql`** with example data: a product family, family assignments and the three levers.
- Add repositories `ScaleDiscountRepository`, `ComboRepository` and `GiftRepository` that load each lever with its children (tiers, items, condition).
- Add tests for the new constraints, the repository round-trips and the foreign-key indexes.
- **Out of scope:** the pricing engine (applying the levers and their order), the outbox relay, HTTP endpoints. The strictly-increasing-tier rule stays app-validated, not enforced by the database.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `data-model`: the lever tables (`scale_discount`, `scale_tier`, `combo`, `combo_item`, `gift_condition`, `gift`) are added.

## Impact

- New Flyway migrations and repositories in `com.marketplaceb2b.persistence.repository`.
- No change to the domain types (already modelled) or to existing behaviour.
