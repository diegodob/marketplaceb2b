# Spec Delta

## MODIFIED Requirements

### Requirement: Product catalog per tenant

`product` SHALL store `tenant_id`, `sku`, `name` and an optional `family_id` referencing `product_family`, with `UNIQUE (tenant_id, sku)`. Products are not shared across tenants.

#### Scenario: Duplicate SKU within a tenant rejected

- **WHEN** two products in the same tenant share a `sku`
- **THEN** the database rejects the second row

#### Scenario: Same SKU allowed across tenants

- **WHEN** two different tenants each insert a product with the same `sku`
- **THEN** both rows are accepted

## ADDED Requirements

### Requirement: Product family

`product_family` SHALL store `tenant_id` and `name`, with `UNIQUE (tenant_id, name)`. A product's family SHALL be this entity, referenced through `product.family_id`.

#### Scenario: Duplicate family name within a tenant rejected

- **WHEN** two product families in the same tenant share a `name`
- **THEN** the database rejects the second row

#### Scenario: Same family name allowed across tenants

- **WHEN** two different tenants each insert a family with the same `name`
- **THEN** both rows are accepted

#### Scenario: Product references an existing family

- **WHEN** a product is inserted with an unknown `family_id`
- **THEN** the database rejects the insert
