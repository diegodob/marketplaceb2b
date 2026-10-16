# Spec Delta

## ADDED Requirements

### Requirement: Pricing pipeline

The engine SHALL apply the applicable commercial levers in a fixed order given by each lever's `priority` (highest first, ties broken by id), and never by minimizing the total. Each applied lever SHALL consume the units it discounts, so a later lever never discounts the same units again.

#### Scenario: Levers apply in priority order

- **WHEN** a cart is priced
- **THEN** the applicable levers are applied from the highest priority to the lowest, and never by trying to minimize the total

#### Scenario: Consumed units are not discounted twice

- **WHEN** a lever has discounted some units
- **THEN** a later lever does not discount those units again

### Requirement: Combo step

A combo SHALL apply when all of its items reach their minimum quantity. Combos SHALL be applied in priority order. A combo SHALL cover the minimum quantities at its total price, units above the minimum SHALL be charged at the regular price, the saving SHALL be reported as a combo discount entry with its origin, and the combo's units SHALL be consumed.

#### Scenario: Combo requires all minimums

- **WHEN** every item of a combo reaches its minimum quantity
- **THEN** the combo applies

#### Scenario: Combo covers the minimums and charges the extras

- **WHEN** a combo applies and an item has more units than its minimum
- **THEN** the minimum units go to the combo and the extra units are charged at the regular price

#### Scenario: Combo is reported with its origin

- **WHEN** a combo applies
- **THEN** the saving appears as a combo discount entry carrying its origin and amount

#### Scenario: Combo units are consumed

- **WHEN** a combo applies to some units
- **THEN** those units are marked consumed so a lower-priority lever does not discount them
