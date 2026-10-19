# marketplaceb2b-api

Multi-tenant B2B marketplace API for the order flow: catalog → cart → checkout (quote) → confirmation → history.

A Java 21 / Spring Boot WebFlux service. This repository contains the order-flow foundation: an immutable domain model, a Flyway-managed schema (H2 in PostgreSQL compatibility mode), a JDBC data-access layer with repositories, an order-pricing engine with a priority-driven lever pipeline (combo, scale and gift) and a credit fit, an idempotent order-confirmation service, and an HTTP endpoint that prices a cart. Quote creation, outbox relay, the order endpoint and the ADR are still pending (see [Scope](#scope)).

## Stack

- **Java 21** (Gradle toolchain)
- **Spring Boot 3.5.x** with **WebFlux** (reactive, Netty)
- **Gradle** (Kotlin DSL) via the committed Gradle wrapper
- **H2** in-memory in **PostgreSQL compatibility mode**; **Flyway** for migrations
- **JDBC** (Spring `JdbcClient`) with repositories; no JPA
- JUnit 5 + `WebTestClient` for tests

## Requirements

- A JDK 21 toolchain available to Gradle. The build requests Java 21 and fails rather than silently compiling for an older target.

## Commands

Run the service:

```
# Windows
gradlew.bat bootRun

# macOS / Linux
./gradlew bootRun
```

The service listens on port `8080` (see `src/main/resources/application.yaml`). On start it runs the Flyway migrations against the in-memory database. Health check:

```
GET http://localhost:8080/api/v1/health   ->   {"status":"UP"}
```

Run the tests (single build-and-test command):

```
# Windows
gradlew.bat clean test

# macOS / Linux
./gradlew clean test
```

## Endpoints

### `POST /api/v1/pricing`

Prices a cart against the tenant's current rules and returns the breakdown, per line and in totals, and — when the payment method uses credit — whether the order fits the customer's available credit.

Request:

```json
{
  "tenantId": 1,
  "customerId": 1,
  "paymentMethodId": 1,
  "lines": [
    { "productId": 1, "qty": 6 },
    { "productId": 2, "qty": 6 }
  ]
}
```

Response `200` (the seeded combo `Gaseosa + Agua x6` applies):

```json
{
  "tenantId": 1,
  "rulesetId": 1,
  "currency": "GTQ",
  "lines": [
    { "productId": 1, "qty": 6, "unitPrice": 4000.0000, "gross": 24000.00, "discount": 11692.31,
      "taxableBase": 12307.69, "taxRate": 12.0000, "tax": 1476.92, "gift": false },
    { "productId": 2, "qty": 6, "unitPrice": 2500.0000, "gross": 15000.00, "discount": 7307.69,
      "taxableBase": 7692.31, "taxRate": 12.0000, "tax": 923.08, "gift": false }
  ],
  "discounts": [ { "origin": "COMBO", "sourceId": 1, "detail": "Gaseosa + Agua x6", "amount": 19000.00 } ],
  "grossSubtotal": 39000.00,
  "totalDiscounts": 19000.00,
  "totalTaxableBase": 20000.00,
  "totalTax": 2400.00,
  "total": 22400.00,
  "creditFit": { "fits": true, "available": 300000.00, "total": 22400.00, "reason": "FITS" }
}
```

Errors carry a stable `code`, a clear `message` and optional `details`. The body never exposes internal identifiers or exception text:

| Status | `code` | When |
|---|---|---|
| 400 | `INVALID_REQUEST` | the cart has no tenant, no lines, or a non-positive quantity (a malformed body included) |
| 422 | `PRODUCT_NOT_SELLABLE` | a cart product has no price in the tenant's current rules |
| 422 | `PAYMENT_METHOD_NOT_FOUND` | the cart references a payment method that does not exist in its tenant |
| 503 | `STORE_PRICING_UNAVAILABLE` | the tenant has no pricing in force |
| 500 | `PRICING_MISCONFIGURED` | a product's tax category has no rate |
| 500 | `INTERNAL_ERROR` | anything else (the detail is logged, not returned) |

## Structure

Packages under `com.marketplaceb2b` (source root `src/main/java`):

```
com.marketplaceb2b
├── MarketplaceB2bApiApplication   # Spring Boot entry point
├── controller                     # HTTP endpoints
├── dto                            # request/response data-transfer objects
├── model                          # domain types (records, enums, Money)
├── repository                     # PersistenceAccess + one repository per aggregate
└── service                        # application logic
    ├── exception                  # domain exceptions
    └── pricing                    # lever pipeline and strategies
```

Schema lives in `src/main/resources/db/migration` (`V1__schema.sql`, `V2__seed.sql`, `V3__product_family.sql`, `V4__levers.sql`, `V5__levers_seed.sql`, `V6__gift_taxable.sql`, `V7__lever_priority.sql`).

## Scope

Implemented:

- Bootable Java 21 Spring Boot WebFlux service; `GET /api/v1/health`
- Immutable, validated domain model (records + Bean Validation): countries, tenants, tax categories, rulesets, tax rates, product families, products, prices, payment methods, customers, credit accounts, quotes/lines, orders, idempotency keys, credit ledger, outbox, and the commercial levers (scale discounts, combos, gifts)
- Flyway schema: `V1__schema.sql` (tables), `V2__seed.sql` (example data), `V3__product_family.sql`, `V4__levers.sql`, `V5__levers_seed.sql`, `V6__gift_taxable.sql`, `V7__lever_priority.sql`
- JDBC data-access layer (`com.marketplaceb2b.repository`: `PersistenceAccess` + repositories) with an append-only guard
- Idempotent, atomic order confirmation (`OrderConfirmationService`): validates the quote, consumes credit when the method uses it, and writes the order, idempotency key, ledger entry and outbox events in one transaction
- Order-pricing engine (`OrderPricingService`): resolves the tenant's current ruleset, reads prices and tax rates, and computes the gross, the discount, the taxable base, the tax and the total with per-line rounding
- Priority-driven lever pipeline (`service.pricing`): independent lever strategies, applied by descending priority with exclusivity over consumed units, plus the **combo**, **scale** and **gift** levers
- Pricing endpoint `POST /api/v1/pricing` (`PricingController`), with validation, non-leaking error responses (`ApiExceptionHandler`) and the credit fit when the payment method uses credit

Pending:

- **Quote creation** (persist an immutable quote from a cart)
- **Outbox relay** (delivery of pending events)
- **HTTP endpoints** for quote creation and `POST /orders` with an idempotency key
- **ADR**
- Optional Angular/Flutter front end

## Design notes

### Lever application order

The engine applies the commercial levers through a **pipeline of strategies** run in a fixed, configured order: **combo → scale → gift → tax**. Each strategy **consumes** the units it discounts, so a later strategy never discounts the same units again — this prevents double-discounting (a combo's units are excluded from the scale). Equal priorities are resolved deterministically: **combo → scale → gift**, a **SKU** scale before a **FAMILY** scale, then the newest lever (greater `id`); the engine never picks the lever that minimizes the total.

- **Combo**: applies only when **all** of its items reach their minimum quantity. Among the applicable combos the engine picks one by a **fixed, documented priority** (never by minimizing the total). The minimum units go to the combo's total price and units **above** the minimum keep the regular price. The saving is reported as a combo entry with its origin.
- **Scale**: targets a single product or a single family; the **reached tier** (the greatest `from_qty` not above the target's available units) applies its percentage to **all** of the target's units. A family scale sums its products' units in the cart.
- **Gift**: evaluated on the **gross cart** (the original units and list value, before the discounts). An applied gift adds a line for its product, marked as a gift, and a `GIFT` discount equal to its value, so no product is paid. Its units never reach a combo or a scale (the gift runs after them and its line is skipped). A **taxable** gift is taxed on its gross (the value before the `GIFT` discount); a non-taxable one pays no tax.
- **Tax**: computed on the taxable base **after** the commercial discounts.

### Rounding policy

Money uses `BigDecimal` (never floating point); monetary amounts are `DECIMAL(19,4)` and rates `DECIMAL(7,4)`. The currency, its decimal scale and the rounding mode come from the `ruleset`. Each line's **taxable base and tax are rounded per line** to the currency scale using the ruleset's rounding mode, and the total is the sum of the already-rounded line values. The arithmetic is encapsulated in a `Money` value object so the rounding cannot be forgotten. A test verifies that stored amounts do not carry more decimals than the currency scale.

### Consistency model

The **quote is immutable** and freezes the ruleset, the prices, the rates and the total, so the quoted and confirmed totals cannot diverge even if prices or rules change between the two moments. Confirmation runs in **a single transaction** (order + idempotency key + credit movement + outbox events) and returns the order number only after the commit; a retry with the same idempotency key returns the original order and never duplicates it or consumes credit twice. Outbound effects use a **transactional outbox** written in the same transaction; delivery is at-least-once and the destination discards duplicates by the event id. The trade-off: at-least-once delivery (no strict exactly-once) and no ordering guarantee.

### Credit fit

When the payment method uses credit, the breakdown carries `creditFit` (`fits`, `available`, `total`, `reason`: `FITS`, `INSUFFICIENT_CREDIT` or `NO_ACCOUNT`), computed on the **final total** after the levers and the tax. The payment method is resolved **tenant-scoped** and the account is read **without locking**; the fit is a **snapshot**, so the confirmation re-checks under the lock and consumes. A customer without an account gets `NO_ACCOUNT` (`fits: false`, `available: 0`) in the preview — the confirmation is the step that refuses the order — and the fit is `null` when the method is absent or does not use credit.

### Known simplifications & rules (for the ADR)

- **Scale discounts target a single product or a single family.** A scale that should span several targets is modelled as several scale discounts. Tiers are cumulative and strictly increasing by starting quantity.
- **Combos are chosen by priority, not by size or price.** A real system would let the distributor set explicit priorities; the engine never minimizes the total for the buyer. Tiers are applications of a fixed order.
- **Combo minimums are a floor.** Each combo item requires a minimum quantity; units **above** the minimum are charged at the regular price (e.g. a minimum of 6 with 8 bought: the 6 go in the combo, the extra 2 are paid).
- **Gift thresholds combine with AND.** A gift condition may require a quantity of a product/family, a minimum amount, or both; when both are present the order must meet both. A disjunction is expressed as two separate gift conditions.
- **A gift is a marked line plus a `GIFT` discount.** The gift product is a line (with a gift mark) whose value is reduced to zero by the `GIFT` entry; a taxable gift is taxed on its gross, so its line's tax differs from its base times its rate. Persisting a gift line for a product that is already paid in the same quote is an open question for quote creation (`quote_line` has `UNIQUE (quote_id, product_id)`).
