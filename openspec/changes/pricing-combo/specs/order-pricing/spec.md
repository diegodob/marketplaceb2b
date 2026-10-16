# Spec Delta

## ADDED Requirements

### Requirement: Pricing pipeline

The engine SHALL apply the commercial levers through a pipeline of strategies run in a fixed, configured priority order. Each strategy SHALL consume the units it discounts, so a later strategy never discounts the same units again.

#### Scenario: Strategies run in a defined order

- **WHEN** a cart is priced
- **THEN** the lever strategies run in a fixed priority order and never by trying to minimize the total

#### Scenario: Consumed units are not discounted twice

- **WHEN** a strategy has discounted some units
- **THEN** a later strategy does not discount those units again

### Requirement: Combo step

A combo SHALL apply when all of its items reach their minimum quantity. Among the applicable combos the engine SHALL pick one by a fixed, documented priority. The combo SHALL cover the minimum quantities at its total price, units above the minimum SHALL be charged at the regular price, the saving SHALL be reported as a combo discount entry with its origin, and the combo's units SHALL be excluded from later steps.

#### Scenario: Combo requires all minimums

- **WHEN** every item of a combo reaches its minimum quantity
- **THEN** the combo applies

#### Scenario: Combo covers the minimums and charges the extras

- **WHEN** a combo applies and an item has more units than its minimum
- **THEN** the minimum units go to the combo and the extra units are charged at the regular price

#### Scenario: Combo is reported with its origin

- **WHEN** a combo applies
- **THEN** the saving appears as a combo discount entry carrying its origin and amount

#### Scenario: Combo units are excluded later

- **WHEN** a combo applies to some units
- **THEN** those units are marked consumed so a later step does not discount them
