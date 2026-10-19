# Tasks

## 1. Data access

- [ ] 1.1 Make `PaymentMethodRepository.find` tenant-scoped (`find(tenantId, id)`) and add `CreditAccountRepository.findByCustomerAndTenant(customerId, tenantId)` without locking; update the callers (the order confirmation passes the quote's tenant) and add multi-tenant tests for both scoped lookups

## 2. Credit fit

- [ ] 2.1 Add `CreditFit` and `CreditFitReason` (`FITS`, `INSUFFICIENT_CREDIT`, `NO_ACCOUNT`) to the DTOs and `PriceBreakdown.creditFit`; implement the fit in `OrderPricingService` (resolve the method, read the account, compare the total, null when not credit) and a `MissingPaymentMethodException` for an id that does not resolve; verify the project compiles

## 3. API and docs

- [ ] 3.1 Map `MissingPaymentMethodException` to 422 (`PAYMENT_METHOD_NOT_FOUND`) in `ApiExceptionHandler`; document the credit fit and the new error code in the README

## 4. Tests

- [ ] 4.1 Base outcomes, each cart carrying one of the discounts to prove the fit is computed on the post-discount total: `FITS` below and exactly at the available credit (with a combo); `INSUFFICIENT_CREDIT` above and when the tax pushes it over (with a scale); `NO_ACCOUNT` for a credit method without an account and without a customer (with a non-taxable gift); `null` for a non-credit method and with no method
- [ ] 4.2 With all three levers: a cart that fits after the combo + scale + a non-taxable gift, and one that does not fit because a taxable gift's tax pushes it over
- [ ] 4.3 Isolation and errors: an unknown method (422), another tenant's method (422), a customer from another tenant (no account -> `NO_ACCOUNT`), the `missing method` exception, and the tenant-scoped lookups
- [ ] 4.4 End to end: the credit fit in the JSON and the 422 `PAYMENT_METHOD_NOT_FOUND`
- [ ] 4.5 Run `gradlew.bat clean test` and confirm the full suite passes with exit code `0`

## Workflow follow-up

- After the maintainer merges the PR, archive the change (`openspec archive credit-fits`).
