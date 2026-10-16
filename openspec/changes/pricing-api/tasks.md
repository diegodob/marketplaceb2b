# Tasks

## 1. Expose the endpoint

- [ ] 1.1 Enable Bean Validation in the main classpath (move `spring-boot-starter-validation` from `testImplementation` to `implementation`) and annotate the cart DTOs: a tenant is required, at least one line, each line a product and a positive quantity
- [ ] 1.2 Add a `PricingController` with `POST /api/v1/pricing` that validates the body, runs `OrderPricingService.price(cart)` off the reactive event loop (`Schedulers.boundedElastic()`) and returns the breakdown; verify the project compiles

## 2. Error mapping

- [ ] 2.1 Add a `@RestControllerAdvice` mapping `NoCurrentRulesetException`, `MissingPriceException` and `MissingTaxRateException` to 422 with a machine-readable error body (`{ "error": <code>, "message": ... }`), and body-validation failures to 400

## 3. End-to-end tests

- [ ] 3.1 Add a `WebTestClient` end-to-end test against the seed: a valid cart is priced (200 and the breakdown, including the seeded combo) and a cart without a combo is priced
- [ ] 3.2 Add the error cases end to end: a product without a price -> 422 with its code; a tenant without a ruleset -> 422; an invalid cart (no lines, or a non-positive quantity) -> 400
- [ ] 3.3 Run `gradlew.bat clean test` and confirm the full suite passes with exit code `0`

## 4. Documentation

- [ ] 4.1 Document the endpoint (method, path, request and response, error codes) in the README

## Workflow follow-up

- After the maintainer merges the PR, archive the change (`openspec archive pricing-api`).
