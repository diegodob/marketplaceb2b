# Proposal

## Why

The take-home's commercial levers — scale discounts, combos and gifts — are not represented in the domain yet; only prices and taxes are. Before any calculation engine exists, the lever definitions need a validated, immutable domain model so a later engine can operate on them.

## What Changes

- Add **Java domain types** for the levers (no tables, no engine):
  - **Scale discount**: a target (a SKU or a `ProductFamily`) plus cumulative, strictly increasing tiers (`fromQty` → percentage).
  - **Combo**: a set of `product` + `minQty` items and a `comboPrice` that is the total for the set.
  - **Gift**: a purchase condition — a quantity of a product/family and/or a minimum amount — plus the gifted product and quantity.
- Turn a product's family into an entity: add **`ProductFamily`** (per tenant) and make **`Product`** reference it by id instead of a free-text name.
- Enforce validity declaratively: percentages `0`–`100`, positive quantities, strictly increasing tiers, non-empty lists, and a target/condition consistent with its type.
- **Out of scope:** persistence (tables/migrations), repositories, the calculation engine, and the order of application.

## Capabilities

### New Capabilities
- `pricing-levers`: the immutable domain types for the commercial levers and their validity rules.

### Modified Capabilities
- None.

## Impact

- New types in `com.marketplaceb2b.model`.
- No schema, migrations or new dependencies.
- The types hang off a `ruleset` (versioned) like prices and taxes, ready for the engine change.
