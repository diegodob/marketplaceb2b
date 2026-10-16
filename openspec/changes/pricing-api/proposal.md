# Proposal

## Why

The pricing engine (`order-pricing`) computes the breakdown in process, but nothing exposes it over HTTP, so a client cannot price a cart. The take-home requires an endpoint (or use case) that receives a cart and returns the breakdown.

## What Changes

- Add `POST /api/v1/pricing`: the request carries a cart (tenant, optional customer and payment method, and lines), the response is the price breakdown (per line and in totals: gross, discount, taxable base, tax and total, plus the discount and gift entries).
- Map engine failures to explicit HTTP client errors: no current ruleset, a product without a price in the ruleset, and a product whose tax category has no rate. The error body is machine-readable.
- Validate the request body: a cart must carry a tenant and at least one line, and each line a product and a positive quantity. An invalid cart is rejected as a client error, never priced.
- Run the (blocking, JDBC) engine off the reactive event loop, so the WebFlux event loop is not blocked.
- Cover the endpoint end to end with tests that go through HTTP against the seeded data.

## Capabilities

### New Capabilities
- `pricing-api`: the HTTP contract to price a cart — the endpoint and its request/response shape, the mapping of engine failures to HTTP errors, and the request validation.

### Modified Capabilities
- None.

## Impact

- New `PricingController` and an error handler in the `controller` layer.
- The cart request DTOs gain validation annotations; `spring-boot-starter-validation` moves from `testImplementation` to `implementation` so the endpoint can validate in runtime.
- A new end-to-end `WebTestClient` test against the seeded data.
