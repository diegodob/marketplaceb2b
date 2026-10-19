# Tasks

## 1. Data access

- [ ] 1.1 Make `PaymentMethodRepository.find` tenant-scoped (`find(tenantId, id)`) and add `CreditAccountRepository.findByCustomer(customerId)` without locking; update the callers (the order confirmation passes the quote's tenant) and add a multi-tenant test for the scoped lookup

## 2. Credit fit

- [ ] 2.1 Add `CreditFit` and `CreditFitReason` (`FITS`, `EXCEEDS_CUPO`, `NO_ACCOUNT`) to the DTOs and `PriceBreakdown.creditFit`; implement the fit in `OrderPricingService` (resolve the method, read the account, compare the total, null when not credit) and a `MissingPaymentMethodException` for an id that does not resolve; verify the project compiles

## 3. API and docs

- [ ] 3.1 Map `MissingPaymentMethodException` to 422 (`PAYMENT_METHOD_NOT_FOUND`) in `ApiExceptionHandler`; document the credit fit and the new error code in the README

## 4. Tests

- [ ] 4.1 Happy path: a credit cart within the cupo (`FITS`) and exactly at the cupo
- [ ] 4.2 Edge cases: above the cupo (`EXCEEDS_CUPO`), no account (`NO_ACCOUNT`), a non-credit method and no method (null), an unknown method (422), another tenant's method (422), and the scoped method lookup
- [ ] 4.3 Run `gradlew.bat clean test` and confirm the full suite passes with exit code `0`

## Workflow follow-up

- After the maintainer merges the PR, archive the change (`openspec archive credit-fits`).
