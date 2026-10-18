# Spec Delta

## ADDED Requirements

### Requirement: Gift step

A gift SHALL apply when its condition is met on the gross cart — the cart's units and its list value, before the other levers. Every met condition SHALL grant its gift, and a gift SHALL never be charged for product.

#### Scenario: A quantity condition is met

- **WHEN** the cart reaches the condition's quantity of its product or family
- **THEN** the gift's units are added to the breakdown

#### Scenario: An amount condition is met

- **WHEN** the cart's gross value reaches the condition's minimum amount
- **THEN** the gift applies

#### Scenario: Quantity and amount combine with AND

- **WHEN** a condition has both a quantity and an amount threshold
- **THEN** the gift applies only when both are met

### Requirement: Gifts accumulate

Every condition met on the gross cart SHALL grant its own gift; a cart that meets several conditions SHALL receive each of them.

#### Scenario: Several conditions are met

- **WHEN** a cart meets two gift conditions
- **THEN** both gifts appear in the breakdown

### Requirement: Gifts do not pay product or trigger other levers

A gift SHALL add free units of its product and SHALL neither consume the cart's units nor make another lever apply; the gifted units SHALL be reported with their quantity.

#### Scenario: Gifted units are reported

- **WHEN** a gift applies
- **THEN** the breakdown carries the gift product and its quantity

#### Scenario: A gift does not trigger another lever

- **WHEN** a gift adds units of a product
- **THEN** those units do not reach a scale or a combo

### Requirement: Gift tax

A taxable gift SHALL add the tax on its list value (its product's unit price times the gifted quantity) at its product's rate to the total and the total tax. A non-taxable gift SHALL add no tax.

#### Scenario: A taxable gift adds tax to the total

- **WHEN** a taxable gift applies
- **THEN** its tax is added to the total tax and the total

#### Scenario: A non-taxable gift adds no tax

- **WHEN** a non-taxable gift applies
- **THEN** the total tax and the total do not change
