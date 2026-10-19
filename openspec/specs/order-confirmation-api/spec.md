# order-confirmation-api Specification

## Purpose
TBD - created by archiving change order-confirmation-api. Update Purpose after archive.

## Requirements

### Requirement: The service confirms a quote over HTTP

The service SHALL expose `POST /api/v1/orders` under `X-Tenant-Id`, requiring an `Idempotency-Key` header and a body with the quote's public id only, and SHALL respond **201** with the order number and the total. The endpoint SHALL delegate to `OrderConfirmationService` without re-pricing and without accepting any amount from the client.

#### Scenario: A confirmed quote returns the order

- **WHEN** a client confirms a known quote with a new key
- **THEN** the service responds 201 with the order number and the stored total

#### Scenario: The body does not carry amounts

- **WHEN** the request body omits any amount
- **THEN** the order is priced from the stored quote, and the client cannot influence the total

### Requirement: The confirmation is idempotent

The service SHALL return the same order for the same key and the same quote, and SHALL reject a key reused for another quote (`IDEMPOTENCY_CONFLICT`, 409) and a quote confirmed under another key (`QUOTE_ALREADY_CONFIRMED`, 409).

#### Scenario: The retry returns the same order

- **WHEN** the same key and the same quote are posted twice
- **THEN** both responses are 201 with the same order and the cupo is consumed once

#### Scenario: The key is not reused across quotes

- **WHEN** a key already used for one quote is posted for another quote
- **THEN** the service responds 409 `IDEMPOTENCY_CONFLICT`

#### Scenario: A quote has one order

- **WHEN** a confirmed quote is posted with a different key
- **THEN** the service responds 409 `QUOTE_ALREADY_CONFIRMED`

### Requirement: The double tap yields one order

When two requests with the same key and quote race, the service SHALL create one order and SHALL return the same 201 to both, re-reading the order after the losing insert conflicts.

#### Scenario: Two simultaneous requests

- **WHEN** two requests confirm the same quote with the same key simultaneously
- **THEN** one order, one consumption and one outbox event exist, and both responses are 201 with the same order

### Requirement: A confirmed order is queryable

The service SHALL expose `GET /api/v1/orders/{number}` under `X-Tenant-Id`, returning the order and its lines for the tenant, and **404** for another tenant or an unknown number.

#### Scenario: The order is read by its tenant

- **WHEN** the tenant that confirmed the order reads it by its number
- **THEN** the service responds 200 with the order and its lines

#### Scenario: Another tenant cannot read it

- **WHEN** another tenant reads the same number
- **THEN** the service responds 404

### Requirement: The confirmation errors are clear

The service SHALL map the confirmation errors: the unknown quote to 404, the insufficient credit, the missing account and the expired quote to 422, the conflicts to 409, and the missing tenant to 401.

#### Scenario: The cupo does not fit

- **WHEN** the stored total exceeds the available credit
- **THEN** the service responds 422 `INSUFFICIENT_CREDIT` and creates no order

#### Scenario: The quote expired

- **WHEN** the quote is past its expiry
- **THEN** the service responds 422 `QUOTE_EXPIRED`
