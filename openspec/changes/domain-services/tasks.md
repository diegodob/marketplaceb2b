# Tasks

Assumptions carried from design.md: repositories in `com.marketplaceb2b.persistence.repository`, service in `com.marketplaceb2b.service`, `@Transactional` over JDBC, idempotency via unique constraint, credit locked `FOR UPDATE`.

## 1. Access layer support

- [x] 1.1 Add `insertReturningId(sql, params)` to `PersistenceAccess` (using `KeyHolder`); verify a unit/integration test inserts a row and gets its generated id

## 2. Repositories

- [x] 2.1 `QuoteRepository.find(tenantId, quoteId)` and `PaymentMethodRepository.find(paymentMethodId)`; verify a test loads a seeded quote and its payment method
- [x] 2.2 `CreditAccountRepository.lockByCustomer(customerId)` (`SELECT ... FOR UPDATE`) and `consume(accountId, amount)`; verify a test debits the balance
- [x] 2.3 `IdempotencyKeyRepository.find(customerId, key)` and `insert(customerId, key, orderId)`; verify a test round-trips a key
- [x] 2.4 `SalesOrderRepository.insert(...)` (returns id) and `findById(id)`; verify a test inserts and reads back an order
- [x] 2.5 `CreditLedgerRepository.insert(accountId, orderId, amount, entryType)` and `OutboxRepository.insertPending(orderId, destination, eventType, payload)`; verify tests insert a consumption and a pending event

## 3. Order confirmation service

- [x] 3.1 Implement `OrderConfirmationService.confirm(tenantId, quoteId, idempotencyKey)` orchestrating the repositories in one `@Transactional` method, returning an `OrderConfirmation` (order id, public number, replayed); verify a happy-path test creates the order, the ledger entry and the two outbox events
- [x] 3.2 Handle the idempotent replay (existing key + same quote returns the order; different quote rejected) and verify both with tests
- [x] 3.3 Handle an expired quote and verify it is rejected with no order created
- [x] 3.4 Handle the credit path (verify available, consume, insufficient rejected leaving nothing) and verify with tests
- [x] 3.5 Verify atomicity: a forced failure mid-confirmation leaves no order, ledger entry or outbox event

## 4. Build verification

- [x] 4.1 Run `gradlew.bat clean test` and confirm the full suite passes with exit code `0`

## Workflow follow-up

- After the maintainer merges the PR, archive the change (`openspec archive domain-services`).
