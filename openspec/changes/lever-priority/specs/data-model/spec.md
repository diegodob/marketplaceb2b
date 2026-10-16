# Spec Delta

## ADDED Requirements

### Requirement: Lever priority

The lever tables `scale_discount`, `combo` and `gift` SHALL each store a `priority` (`INTEGER`, NOT NULL). A higher `priority` is applied first; equal priorities are resolved deterministically by `id`.

#### Scenario: Every lever carries a priority

- **WHEN** a lever row is inspected
- **THEN** it has a non-null `priority`

#### Scenario: Levers are ordered by priority

- **WHEN** the levers of a ruleset are ordered
- **THEN** they are ordered by descending `priority`, ties broken by `id`
