# Spec Delta

## Purpose

Confirms a quote into a sales order exactly once: an idempotent, atomic operation that consumes credit when the payment method requires it and enqueues the outbound events, so a `201` guarantees the order exists and repeats never duplicate it.

## ADDED Requirements

### Requirement: Confirmation creates an order from the quote

Confirmation SHALL create a sales order derived from the quote, using the quote's stored values and never recalculating the amount.

#### Scenario: Order is created from a quote

- **WHEN** a valid quote is confirmed
- **THEN** a sales order is created referencing that quote, and its amount is read from the quote

#### Scenario: A quote already turned into an order is not reused

- **WHEN** a quote that already has an order is confirmed again with a new idempotency key
- **THEN** the confirmation is rejected

### Requirement: Confirmation is idempotent

Confirmation SHALL be idempotent per customer and idempotency key: a replay with the same key and the same quote returns the original order and changes nothing.

#### Scenario: Replay returns the original order

- **WHEN** the same customer confirms the same quote with an idempotency key that was already used
- **THEN** the original order is returned and no new order, ledger entry or outbox event is created

#### Scenario: Replay does not consume credit twice

- **WHEN** a credit order is confirmed again with the same key and quote
- **THEN** the credit account is not debited a second time

#### Scenario: Same key for a different quote is rejected

- **WHEN** a customer confirms a different quote with an idempotency key they already used
- **THEN** the confirmation is rejected

### Requirement: Expired quotes are rejected

Confirmation SHALL refuse a quote whose expiry instant has passed.

#### Scenario: Expired quote cannot be confirmed

- **WHEN** a quote whose expiry is in the past is confirmed
- **THEN** the confirmation is rejected and no order is created

### Requirement: Credit is checked and consumed when the method uses credit

When the quote's payment method uses credit, confirmation SHALL verify the customer's available credit before debiting it; insufficient credit SHALL reject the confirmation without leaving an order.

#### Scenario: Available credit is consumed

- **WHEN** a quote paid with a credit method is confirmed and the available balance covers the total
- **THEN** the balance is reduced by the total and a consumption entry is written

#### Scenario: Insufficient credit is rejected

- **WHEN** the available balance is less than the quote total
- **THEN** the confirmation is rejected and no order, ledger entry or balance change remains

#### Scenario: A non-credit method does not touch credit

- **WHEN** the payment method does not use credit
- **THEN** no credit account is read or written

### Requirement: Confirmation is atomic

Order, idempotency key, credit movement and outbound events SHALL be persisted together in one transaction, or not at all; the order number is returned only after the commit.

#### Scenario: Partial failure leaves nothing behind

- **WHEN** any step of the confirmation fails
- **THEN** no order, idempotency key, ledger entry or outbox event remains persisted

### Requirement: Confirmation enqueues the outbound events

Confirmation SHALL enqueue two pending events for the order, one for the push destination and one for the ERP.

#### Scenario: Two pending events are enqueued

- **WHEN** an order is confirmed
- **THEN** two `PENDING` events exist for it, `PUSH` and `ERP`, both of type `ORDER_CONFIRMED`

### Requirement: The service accesses data only through repositories

The confirmation service SHALL obtain and persist data through repositories and SHALL not issue SQL directly.

#### Scenario: Data access is delegated

- **WHEN** the confirmation service is inspected
- **THEN** it depends on repository types and contains no SQL
