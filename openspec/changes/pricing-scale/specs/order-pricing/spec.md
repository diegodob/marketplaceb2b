# Spec Delta

## ADDED Requirements

### Requirement: Scale step

A scale SHALL apply when the available units of its target reach its first tier. The reached tier — the one with the greatest `from_qty` not above those units — defines the percentage, and that percentage SHALL apply to all of the target's available units.

#### Scenario: First tier reached

- **WHEN** the target's available units reach the first tier
- **THEN** the first tier's percentage applies to all the target's units

#### Scenario: Higher tier reached

- **WHEN** the target's available units reach a higher tier
- **THEN** that tier's percentage applies to all the target's units, not only to the units above its `from_qty`

#### Scenario: Below the first tier

- **WHEN** the target's available units are below the first tier
- **THEN** the scale does not apply and the units keep the list price

### Requirement: Scale targets a SKU or a family

A SKU scale SHALL cover only its product. A FAMILY scale SHALL cover every cart line whose product belongs to that family, and SHALL reach its tier on their combined available units.

#### Scenario: A SKU scale covers only its product

- **WHEN** a SKU scale applies
- **THEN** only that product's line is discounted

#### Scenario: A family scale sums its products

- **WHEN** the cart holds units of several products of the family
- **THEN** their units are summed to reach the tier and all of them are discounted

### Requirement: Scale reporting and exclusivity

An applied scale SHALL consume the units it discounts and SHALL report the saving as a scale entry carrying the source lever (its type and id) and the amount. Because the units are consumed, a lower-priority scale on the same target SHALL not discount them again.

#### Scenario: Scale units are consumed

- **WHEN** a scale applies to some units
- **THEN** those units are marked consumed so a lower-priority lever does not discount them

#### Scenario: Scale is reported with its origin

- **WHEN** a scale applies
- **THEN** the saving appears as a scale entry carrying the source lever (its type and id) and the amount
