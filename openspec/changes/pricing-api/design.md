# Design

## Context

See proposal.md — Why. The pricing engine is in process: `OrderPricingService.price(Cart)` is a **blocking** call over JDBC. The app runs on **WebFlux** (`spring-boot-starter-webflux`) and today exposes only `GET /api/v1/health`; the controller test precedent (`HealthControllerTest`) boots `@SpringBootTest(webEnvironment = RANDOM_PORT)` and drives a real HTTP call with `WebTestClient`. `spring-boot-starter-validation` is currently **test-only** (`build.gradle.kts`), so Bean Validation does not run in the main classpath yet.

## Goals / Non-Goals

**Goals:**
- One HTTP contract to price a cart, reusing the engine's output as-is.
- Explicit client errors for carts the engine cannot price, and for invalid requests.
- End-to-end coverage through real HTTP against the seeded data.

**Non-Goals:**
- Authentication and resolving the tenant from the request (the take-home has no auth; the tenant travels in the body — see Risks).
- Persisting a quote or an order (its own slice), streaming, or partial pricing.

## Decisions

**`POST /api/v1/pricing`, request = the cart, response = the existing breakdown.** Reuse the `Cart` request DTO and the `PriceBreakdown` response DTO, so the HTTP contract and the engine cannot drift. Alternative: a dedicated transport DTO per layer — rejected: duplicates the contract and drifts from the engine.

**Run the blocking engine off the event loop.** The handler wraps the call as `Mono.fromCallable(() -> service.price(cart)).subscribeOn(Schedulers.boundedElastic())`. Alternative: make the engine reactive — rejected: JDBC is blocking and the whole pricing slice is synchronous; a bridge is the honest boundary.

**Map engine failures with a `@RestControllerAdvice`.** `NoCurrentRulesetException`, `MissingPriceException` and `MissingTaxRateException` → **422 Unprocessable Entity** with a machine-readable body `{ "error": <code>, "message": ... }`; a body that fails validation → **400**. Alternatives: 500 (rejected — these are client-caused); 404 (rejected — nothing is missing, the cart cannot be priced); two identical codes (rejected — the client needs to tell "bad request" from "unpriceable cart").

**Validate the request body.** Annotate the cart DTOs (a tenant is required, at least one line, each line a product and a positive quantity) and use `@Valid`, so an invalid cart is a 400 and never reaches the engine. Requires moving `spring-boot-starter-validation` from `testImplementation` to `implementation`. Alternative: hand-rolled checks in the controller — rejected: duplicates the model's rules.

## Risks / Trade-offs

- **The tenant travels in the request body** (no auth yet) → a client could price another tenant's cart. Acknowledged: auth and tenant resolution are a separate slice; keeping the tenant explicit means it can move to a header/credential later without changing the engine.
- **Blocking bridge** → the bounded-elastic scheduler bounds concurrency; a real system would want non-blocking data access. Documented; acceptable for the take-home.
- **The error body becomes a contract** → keep the error codes stable and assert them in the end-to-end test.

## Migration Plan

None — this adds an endpoint; no data or behavior changes elsewhere.

## Open Questions

- None.
