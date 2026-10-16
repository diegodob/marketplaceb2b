# Spec Delta

## MODIFIED Requirements

### Requirement: Gift storage

`gift_condition` SHALL store an optional `product_id`/`family_id` target (never both), an optional `min_qty` (`> 0`) and an optional `min_amount` (`>= 0`), with at least one threshold present and a quantity threshold naming exactly one target. `gift` SHALL store `ruleset_id`, a `condition_id`, a `gift_product_id`, a positive `gift_qty` and a `taxable` flag (`NOT NULL`).

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

#### Scenario: Gifted units carry their tax treatment

- **WHEN** a gift is inspected
- **THEN** it carries a non-null `taxable` flag
