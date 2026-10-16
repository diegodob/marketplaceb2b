# Spec Delta

## ADDED Requirements

### Requirement: Scale discount storage

`scale_discount` SHALL store `ruleset_id`, `target_type` (`SKU` or `FAMILY`) and exactly one of `product_id` or `family_id`, consistent with `target_type`. `scale_tier` SHALL store `scale_discount_id`, `from_qty` (`> 0`) and `percentage` (`0`–`100`). The tiers are cumulative and strictly increasing, but the increasing order is validated in the application, not by the database.

#### Scenario: Target must match the type

- **WHEN** a scale discount is stored with `target_type = 'SKU'` and no `product_id`, or with `target_type = 'FAMILY'` and a `product_id`
- **THEN** the database rejects the row

#### Scenario: Tier ranges are enforced

- **WHEN** a tier has `from_qty` of `0` or less, or a `percentage` outside `0`–`100`
- **THEN** the database rejects the row

#### Scenario: Tier order is not a database constraint

- **WHEN** tiers of one scale discount are not strictly increasing
- **THEN** the database accepts them (the application validates the order)

### Requirement: Combo storage

`combo` SHALL store `ruleset_id`, `name` and a non-negative `combo_price`. `combo_item` SHALL store `combo_id`, `product_id` and a positive `min_qty`, with at most one row per product in a combo.

#### Scenario: Negative combo price rejected

- **WHEN** a combo with a negative `combo_price` is stored
- **THEN** the database rejects the row

#### Scenario: Non-positive minimum rejected

- **WHEN** a combo item with `min_qty` of `0` or less is stored
- **THEN** the database rejects the row

#### Scenario: One row per product per combo

- **WHEN** two combo items share the same `combo_id` and `product_id`
- **THEN** the database rejects the second row

### Requirement: Gift storage

`gift_condition` SHALL store an optional `product_id`/`family_id` target (never both), an optional `min_qty` (`> 0`) and an optional `min_amount` (`>= 0`), with at least one threshold present and a quantity threshold naming exactly one target. `gift` SHALL store `ruleset_id`, a `condition_id`, a `gift_product_id` and a positive `gift_qty`.

#### Scenario: A condition needs a threshold

- **WHEN** a gift condition has neither `min_qty` nor `min_amount`
- **THEN** the database rejects the row

#### Scenario: A quantity threshold needs exactly one target

- **WHEN** a gift condition has a `min_qty` but no `product_id`/`family_id`, or names both targets
- **THEN** the database rejects the row

#### Scenario: Negative amount rejected

- **WHEN** a gift condition has a negative `min_amount`
- **THEN** the database rejects the row

#### Scenario: Non-positive gift quantity rejected

- **WHEN** a gift with a `gift_qty` of `0` or less is stored
- **THEN** the database rejects the row
