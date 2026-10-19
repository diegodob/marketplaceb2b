# Spec Delta

## ADDED Requirements

### Requirement: Gift step

A gift SHALL apply when its condition is met on the gross cart — the cart's units and list value, before the other levers. It SHALL add a line for the gift product, marked as a gift, and a GIFT discount equal to the line's value, so the line's net base is zero.

#### Scenario: A quantity condition is met

- **WHEN** the cart reaches the condition's quantity of its product or family
- **THEN** the gift's line is added to the breakdown

#### Scenario: An amount condition is met

- **WHEN** the cart's gross value reaches the condition's minimum amount
- **THEN** the gift applies

#### Scenario: Quantity and amount combine with AND

- **WHEN** a condition has both a quantity and an amount threshold
- **THEN** the gift applies only when both are met

#### Scenario: The gift adds a marked line

- **WHEN** a gift applies
- **THEN** the line carries the gift product, its quantity and a gift mark, plus a GIFT discount equal to its value

### Requirement: Gifts accumulate

Every condition met on the gross cart SHALL grant its own gift; a cart that meets several conditions SHALL receive each of them.

#### Scenario: Several conditions are met

- **WHEN** a cart meets two gift conditions
- **THEN** both gifts appear in the breakdown

### Requirement: Gift lines are isolated

A gift line SHALL not consume the cart's units and SHALL not make the combo or the scale apply. The gift SHALL run after them, and the other strategies SHALL skip gift lines.

#### Scenario: A gift line does not reach a scale

- **WHEN** a gift adds units of a product
- **THEN** those units do not reach a scale or a combo

#### Scenario: The gift is visible as a line

- **WHEN** a gift applies
- **THEN** the breakdown carries it as a line marked as a gift

### Requirement: Gift tax

A taxable gift's line SHALL be taxed on its gross — the value before its GIFT discount — and that tax SHALL be added to the total tax and the total. A non-taxable gift's line SHALL pay no tax.

#### Scenario: A taxable gift is taxed on its value

- **WHEN** a taxable gift applies
- **THEN** its line's tax is computed on the gross, not on the zero net base, and is added to the total

#### Scenario: A non-taxable gift pays no tax

- **WHEN** a non-taxable gift applies
- **THEN** its line's tax is zero
