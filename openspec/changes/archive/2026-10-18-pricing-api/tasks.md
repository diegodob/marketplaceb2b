# Tasks

## 1. Expose the endpoint

- [x] 1.1 Enable Bean Validation in the main classpath (move `spring-boot-starter-validation` from `testImplementation` to `implementation`) and annotate the cart DTOs: a tenant is required, at least one line, each line a product and a positive quantity
- [x] 1.2 Add a `PricingController` with `POST /api/v1/pricing` that validates the body, runs `OrderPricingService.price(cart)` off the reactive event loop (`Schedulers.boundedElastic()`) and returns the breakdown; verify the project compiles

## 2. Error mapping

- [x] 2.1 Add a `@RestControllerAdvice`: `MissingPriceException` -> 422 `PRODUCT_NOT_SELLABLE`, `NoCurrentRulesetException` -> 503 `STORE_PRICING_UNAVAILABLE`, `MissingTaxRateException` -> 500 `PRICING_MISCONFIGURED`, body-validation failures (`WebExchangeBindException`) -> 400 `INVALID_REQUEST` (with field details) and any other exception -> 500 `INTERNAL_ERROR` (logged). Every body is `{ code, message, details? }` with a stable code and a clear Spanish message, never `e.getMessage()`

## 3. End-to-end tests

- [x] 3.1 Add a `WebTestClient` end-to-end test against the seed: a valid cart is priced (200 and the breakdown, including the seeded combo) and a cart without a combo is priced
- [x] 3.2 Add the error cases end to end: a product without a price -> 422 `PRODUCT_NOT_SELLABLE`; a tenant without pricing -> 503 `STORE_PRICING_UNAVAILABLE`; a cart with no lines and a line with a non-positive quantity -> 400 `INVALID_REQUEST`; assert each body carries no internal identifiers or exception text
- [x] 3.3 Run `gradlew.bat clean test` and confirm the full suite passes with exit code `0`

## 4. Documentation

- [x] 4.1 Document the endpoint (method, path, request and response, error codes) in the README

## Workflow follow-up

- After the maintainer merges the PR, archive the change (`openspec archive pricing-api`).
