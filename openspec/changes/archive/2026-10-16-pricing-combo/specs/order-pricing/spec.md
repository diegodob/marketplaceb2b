# Spec Delta

## ADDED Requirements

### Requirement: Pricing pipeline

The engine SHALL apply the applicable commercial levers in a fixed order given by each lever's `priority` (highest first, ties broken by id), and never by minimizing the total. Each applied lever SHALL consume the units it discounts, so a later lever never discounts the same units again. A line's taxable base SHALL be its list gross minus its discounts, and the tax SHALL be computed on that base.

#### Scenario: Levers apply in priority order

- **WHEN** a cart is priced
- **THEN** the applicable levers are applied from the highest priority to the lowest, and never by trying to minimize the total

#### Scenario: Consumed units are not discounted twice

- **WHEN** a lever has discounted some units
- **THEN** a later lever does not discount those units again

### Requirement: Combo step

A combo SHALL apply when every item still has enough available units to reach its minimum quantity, and combos SHALL be applied in priority order. A combo SHALL cover the minimum quantities at its total price by recording, per covered line, a discount (its share of the difference) while the line keeps its list gross; units above the minimum SHALL be charged at the regular price, and the combo's units SHALL be consumed. The saving SHALL be reported as a combo discount entry.

#### Scenario: Combo requires all minimums

- **WHEN** every item of a combo reaches its minimum quantity
- **THEN** the combo applies

#### Scenario: Combo covers the minimums and charges the extras

- **WHEN** a combo applies and an item has more units than its minimum
- **THEN** the minimum units go to the combo and the extra units are charged at the regular price

#### Scenario: Combo is reported with its origin

- **WHEN** a combo applies
- **THEN** the saving appears as a combo discount entry carrying the source lever (its type and id) and the amount

#### Scenario: Combo units are consumed

- **WHEN** a combo applies to some units
- **THEN** those units are marked consumed so a lower-priority lever does not discount them

#### Scenario: Combo lowers the taxable base

- **WHEN** a combo applies
- **THEN** each covered line keeps its list gross, its taxable base is that gross minus its discount share, and the tax is computed on that base
