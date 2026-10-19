# Spec Delta

## ADDED Requirements

### Requirement: Tenant-consistent references

`customer` and `quote` SHALL be uniquely identified by `(tenant_id, id)`. `quote` and `sales_order` SHALL reference their customer with `(tenant_id, customer_id)`, and `sales_order` SHALL reference its quote with `(tenant_id, quote_id)`, enforced by foreign keys, so a row never points at another tenant's customer or quote.

#### Scenario: A cross-tenant customer is rejected

- **WHEN** a quote or an order references a customer of another tenant
- **THEN** the database rejects the row

#### Scenario: A cross-tenant quote is rejected

- **WHEN** an order references a quote of another tenant
- **THEN** the database rejects the row

### Requirement: Applied discounts and gifts on a quote

`quote_line_discount` SHALL store the applied discounts of a quote line (a label, an amount and exactly one of the combo, scale or gift lever it came from) and `quote_gift` SHALL store each gifted set of a quote (its gift, product, quantity, taxable flag, value and tax).

#### Scenario: The applied entries are stored

- **WHEN** a quote is persisted
- **THEN** its discounts are stored per line and its gifts per gift, each traceable to the lever that produced it
