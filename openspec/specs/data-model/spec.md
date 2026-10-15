# data-model Specification

## Purpose
Defines the relational data model that persists the multi-tenant B2B marketplace order flow. Its constraints are the enforceable form of the domain invariants: exact money, versioned price/rule snapshots, an immutable quote → order path, append-only credit movements, idempotent confirmation and a transactional outbox.

## Requirements

### Requirement: Surrogate primary keys

Every table SHALL have `id BIGINT GENERATED ALWAYS AS IDENTITY` as its only primary key. No primary key SHALL be composite or natural, and every foreign key SHALL reference the referenced table's `id`.

#### Scenario: Surrogate key on every table

- **WHEN** the schema is inspected
- **THEN** each table has a single-column identity `id` primary key and no composite or natural primary key

#### Scenario: Foreign keys target id

- **WHEN** a foreign key is defined
- **THEN** it references the target table's `id` column

### Requirement: Country and tenant

`country` SHALL store `code` (ISO, UNIQUE) and `name`. `tenant` SHALL store `name` (UNIQUE) and `country_id` referencing `country`. A tenant is a brand in exactly one country.

#### Scenario: Duplicate country code rejected

- **WHEN** two rows with the same `country.code` are inserted
- **THEN** the database rejects the second row

#### Scenario: Duplicate tenant name rejected

- **WHEN** two rows with the same `tenant.name` are inserted
- **THEN** the database rejects the second row

#### Scenario: Tenant references an existing country

- **WHEN** a tenant is inserted with an unknown `country_id`
- **THEN** the database rejects the insert

### Requirement: Tax category

`tax_category` SHALL store `code` (UNIQUE) and `name`. It SHALL NOT store any percentage; the rate depends on the ruleset version (`tax_rate`).

#### Scenario: Duplicate tax category code rejected

- **WHEN** two rows with the same `tax_category.code` are inserted
- **THEN** the database rejects the second row

### Requirement: Immutable versioned ruleset

`ruleset` SHALL store `tenant_id`, `version`, `currency`, `currency_scale`, `rounding_mode` and `effective_from`, and SHALL be immutable per tenant (a change creates a new version). `UNIQUE (tenant_id, version)` and `UNIQUE (tenant_id, effective_from)` SHALL hold.

#### Scenario: Duplicate version per tenant rejected

- **WHEN** two rulesets with the same `tenant_id` and `version` are inserted
- **THEN** the database rejects the second row

#### Scenario: Two rulesets with same effective_from rejected

- **WHEN** two rulesets for the same tenant share an `effective_from`
- **THEN** the database rejects the second row

#### Scenario: Current ruleset resolves to the newest past version

- **WHEN** the ruleset in force is resolved for a tenant at a given instant
- **THEN** it is the version with the greatest `effective_from` that is not in the future

### Requirement: Tax rate per ruleset

`tax_rate` SHALL store `ruleset_id`, `tax_category_id` and `rate` constrained to `0`–`100`, with `UNIQUE (ruleset_id, tax_category_id)`. The percentage varies per ruleset version, not globally.

#### Scenario: One rate per category per ruleset

- **WHEN** two `tax_rate` rows share the same `ruleset_id` and `tax_category_id`
- **THEN** the database rejects the second row

#### Scenario: Out-of-range rate rejected

- **WHEN** a `tax_rate.rate` below `0` or above `100` is inserted
- **THEN** the database rejects the row

### Requirement: Product catalog per tenant

`product` SHALL store `tenant_id`, `sku`, `name` and optional `family`, with `UNIQUE (tenant_id, sku)`. Products are not shared across tenants.

#### Scenario: Duplicate SKU within a tenant rejected

- **WHEN** two products in the same tenant share a `sku`
- **THEN** the database rejects the second row

#### Scenario: Same SKU allowed across tenants

- **WHEN** two different tenants each insert a product with the same `sku`
- **THEN** both rows are accepted

### Requirement: Versioned product price and tax category

`product_price` SHALL store `ruleset_id`, `product_id`, `tax_category_id` and `unit_price` (`>= 0`), with `UNIQUE (ruleset_id, product_id)`. A product's net unit price and tax category belong to a ruleset version, not to `product`.

#### Scenario: One price per product per ruleset

- **WHEN** two `product_price` rows share the same `ruleset_id` and `product_id`
- **THEN** the database rejects the second row

#### Scenario: Negative unit price rejected

- **WHEN** a `product_price.unit_price` below `0` is inserted
- **THEN** the database rejects the row

#### Scenario: Missing price is absence, not zero

- **WHEN** a product has no `product_price` row for the ruleset being used
- **THEN** there is no row to read, so the product is treated as not quotable rather than priced at `0`

### Requirement: Payment method with credit indicator

`payment_method` SHALL store `tenant_id`, `code`, `name` and `uses_credit` (`NOT NULL`), with `UNIQUE (tenant_id, code)`. Whether a method consumes credit SHALL be decided by `uses_credit`, never by comparing `code` to a fixed value.

#### Scenario: Duplicate payment code within a tenant rejected

- **WHEN** two payment methods in the same tenant share a `code`
- **THEN** the database rejects the second row

#### Scenario: uses_credit is mandatory

- **WHEN** a payment method is inserted without `uses_credit`
- **THEN** the database rejects the row

### Requirement: Customer belongs to a tenant

`customer` SHALL store `tenant_id` and `name`, and SHALL NOT store a country of its own. A customer belongs to a tenant, and therefore to the tenant's country.

#### Scenario: Customer is scoped to a tenant

- **WHEN** a customer is inspected
- **THEN** it carries `tenant_id` and has no country column

### Requirement: Fiscal identifier type

`fiscal_id_type` SHALL store `country_id`, `code` and `name`, with `UNIQUE (country_id, code)`. Valid types are country-specific (for example `CUIT` in Argentina).

#### Scenario: One fiscal identifier type code per country

- **WHEN** two `fiscal_id_type` rows share the same `country_id` and `code`
- **THEN** the database rejects the second row

### Requirement: Customer fiscal identifier

`customer` SHALL store a `fiscal_id` (NOT NULL) and a `fiscal_id_type_id` referencing `fiscal_id_type`. The fiscal identifier is alphanumeric and that rule is enforced in Java, not by the database: the column carries no format `CHECK`. It is not required to be unique.

#### Scenario: Fiscal id and type are present

- **WHEN** a customer is inspected
- **THEN** it carries a non-null `fiscal_id` and a `fiscal_id_type_id`

#### Scenario: The database does not enforce the alphanumeric format

- **WHEN** the `fiscal_id` column definition is inspected
- **THEN** it is a plain character column with no format `CHECK`

### Requirement: Credit account with bounded balance

`credit_account` SHALL store `customer_id` (UNIQUE), `currency`, `credit_limit` and `available`, with `CHECK (available >= 0 AND available <= credit_limit)`. The current balance lives here; its history lives in `credit_ledger`.

#### Scenario: One credit account per customer

- **WHEN** a second credit account is inserted for the same `customer_id`
- **THEN** the database rejects it

#### Scenario: Negative available rejected

- **WHEN** `available` below `0` is stored
- **THEN** the database rejects the row

#### Scenario: available above credit_limit rejected

- **WHEN** `available` greater than `credit_limit` is stored
- **THEN** the database rejects the row

### Requirement: Immutable quote

`quote` SHALL store `public_id` (UNIQUE, non-guessable), `tenant_id`, `customer_id`, `ruleset_id`, `payment_method_id`, `total`, `created_at` and `expires_at`, and SHALL never be updated. `total` is the amount promised to the buyer; `created_at` records when it was generated; `expires_at` is configurable (24 hours after `created_at` by default) rather than hard-coded.

#### Scenario: Two quotes cannot share a public id

- **WHEN** two quotes share `public_id`
- **THEN** the database rejects the second row

#### Scenario: Quote total equals the sum of its lines

- **WHEN** a quote is stored
- **THEN** its `total` equals the sum over its lines of `taxable_base + tax`

#### Scenario: Expired quote cannot be confirmed

- **WHEN** a quote whose `expires_at` is in the past is presented for confirmation
- **THEN** confirmation is refused and a new quote is required

### Requirement: Immutable quote lines with frozen price and rate

`quote_line` SHALL store `quote_id`, `product_id`, `qty` (`> 0`), `unit_price`, `taxable_base`, `tax_rate` and `tax`, with `UNIQUE (quote_id, product_id)`. `unit_price` and `tax_rate` are frozen copies; `taxable_base` and `tax` are rounded engine results.

#### Scenario: One line per product per quote

- **WHEN** two `quote_line` rows share the same `quote_id` and `product_id`
- **THEN** the database rejects the second row

#### Scenario: Non-positive quantity rejected

- **WHEN** a `quote_line.qty` of `0` or less is inserted
- **THEN** the database rejects the row

### Requirement: Sales order derived from exactly one quote

`sales_order` SHALL store `public_number` (UNIQUE), `tenant_id`, `quote_id` (UNIQUE), `customer_id`, `created_at` and `deleted_at`. It SHALL NOT have lines, `status` or a total of its own: the amount is read from its quote. A modification is a new order plus `deleted_at` on the previous one.

#### Scenario: A quote cannot originate two orders

- **WHEN** a second `sales_order` is inserted with an already-used `quote_id`
- **THEN** the database rejects it

#### Scenario: Two orders cannot share a public number

- **WHEN** two orders share `public_number`
- **THEN** the database rejects the second row

#### Scenario: An order has no total or status column

- **WHEN** the `sales_order` columns are inspected
- **THEN** there is no total column, no line table and no status column; only `deleted_at` marks it inactive

### Requirement: Idempotent confirmation key

`idempotency_key` SHALL store `customer_id`, `idem_key` and `sales_order_id`, with `UNIQUE (customer_id, idem_key)`. The same key with the same quote returns the original order without inserting; the same key with a different quote is rejected.

#### Scenario: Duplicate key per customer rejected

- **WHEN** two `idempotency_key` rows share the same `customer_id` and `idem_key`
- **THEN** the database rejects the second row

#### Scenario: Retry with same key and quote returns the original order

- **WHEN** confirmation is replayed with an existing key and the quote of the stored order
- **THEN** the original order is returned and nothing is inserted

#### Scenario: Same key with a different quote rejected

- **WHEN** confirmation is submitted with an existing key but a different `quote_id`
- **THEN** the request is rejected

### Requirement: Append-only credit ledger

`credit_ledger` SHALL store `credit_account_id`, `sales_order_id`, `amount` (signed), `entry_type` (`CONSUMPTION` or `REVERSAL`) and `created_at`, with `UNIQUE (sales_order_id, entry_type)`. Rows SHALL never be updated or deleted, and `REVERSAL` is the only way to correct a movement.

#### Scenario: One consumption per order

- **WHEN** two `credit_ledger` rows share the same `sales_order_id` and `entry_type` `CONSUMPTION`
- **THEN** the database rejects the second row

#### Scenario: Ledger rows cannot be modified or deleted

- **WHEN** an update or delete is attempted on `credit_ledger`
- **THEN** the database refuses the operation

#### Scenario: Ledger explains the available balance

- **WHEN** a credit account's movements are summed
- **THEN** `credit_limit + sum(amount) = available` holds

### Requirement: Transactional outbox

`outbox` SHALL store `sales_order_id`, `destination` (`PUSH` or `ERP`), `event_type` (`ORDER_CONFIRMED` or `ORDER_CANCELLED`), `payload` (text), `status`, `attempts`, `next_attempt_at`, `last_error`, `created_at` and `sent_at`, with `UNIQUE (sales_order_id, destination, event_type)`. `payload` is the only free-text domain field.

#### Scenario: One event per order, destination and type

- **WHEN** two `outbox` rows share `sales_order_id`, `destination` and `event_type`
- **THEN** the database rejects the second row

#### Scenario: Confirmation enqueues two effects

- **WHEN** an order is confirmed
- **THEN** two `PENDING` rows exist for it, one for `PUSH` and one for `ERP`, both `ORDER_CONFIRMED`

#### Scenario: Duplicate delivery is discarded by the consumer

- **WHEN** the same event is delivered more than once
- **THEN** the destination discards repeats using the event's `id` as the message identifier

### Requirement: Exact money and rates

Monetary columns SHALL use `DECIMAL(19,4)` and tax-rate columns SHALL use `DECIMAL(7,4)`; no monetary or rate column SHALL use a floating-point type. Every stored monetary line value SHALL be rounded to `ruleset.currency_scale` using `ruleset.rounding_mode` before it is written; the supported modes are `HALF_UP` and `HALF_EVEN`.

#### Scenario: No floating-point money columns

- **WHEN** the schema is inspected
- **THEN** no monetary or rate column uses `FLOAT`, `REAL` or `DOUBLE`

#### Scenario: Stored amounts match the currency scale

- **WHEN** stored monetary values are checked against their currency's scale
- **THEN** no value carries more decimal places than `ruleset.currency_scale`

### Requirement: Derived values are not stored

Line totals, subtotals, tax totals and the order amount SHALL NOT be stored; they are computed when a response is assembled or read from the quote. Only engine results (`taxable_base`, `tax`) and frozen inputs (`unit_price`, `tax_rate`) are persisted.

#### Scenario: No derived totals in the schema

- **WHEN** the schema is inspected
- **THEN** there is no `line_total`, `subtotal`, `tax_total` or `sales_order.total` column

### Requirement: Row-shaped domain, no fingerprints

Domain data SHALL be modeled as rows; the only free-text field is `outbox.payload`. There SHALL be no JSON columns for domain data and no `content_hash` or `request_fingerprint` columns.

#### Scenario: No domain JSON, hashes or fingerprints

- **WHEN** the schema is inspected
- **THEN** no column stores domain data as JSON and no `content_hash` or `request_fingerprint` column exists

### Requirement: Multi-tenant column isolation

Shared tables SHALL carry a `tenant_id` column and each tenant SHALL see only its own rows. A stored schema does not by itself enforce filtering; the access layer must always scope by tenant.

#### Scenario: Isolation with two tenants and the same SKU

- **WHEN** two tenants each hold the same `sku` and data is read for one tenant
- **THEN** only that tenant's rows are returned

### Requirement: Explicit foreign-key indexes

Every foreign-key column SHALL have an explicit index, because the database does not create them automatically.

#### Scenario: Every foreign key is indexed

- **WHEN** the schema is inspected
- **THEN** each foreign-key column has a corresponding index
