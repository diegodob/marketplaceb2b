# Spec Delta

## MODIFIED Requirements

### Requirement: Multi-tenant column isolation

Shared tables SHALL carry a `tenant_id` column and each tenant SHALL see only its own rows. A stored schema does not by itself enforce filtering; the access layer must always scope by tenant. Every query that lists or filters products — or any other tenant-owned entity — SHALL be scoped by tenant in the access layer, and SHALL be covered by a multi-tenant test that proves rows of another tenant are never returned.

#### Scenario: Isolation with two tenants and the same SKU

- **WHEN** two tenants each hold the same `sku` and data is read for one tenant
- **THEN** only that tenant's rows are returned

#### Scenario: Product filters are tenant-scoped and tested

- **WHEN** a product listing or filter is added for a tenant
- **THEN** the query is scoped by tenant in the access layer and a multi-tenant test proves another tenant's products are not returned
