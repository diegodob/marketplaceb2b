# Design

## Context

The repository currently contains only the OpenSpec scaffold; there is no build file, source tree, or test harness. This change establishes the first application code for the marketplace B2B service. See proposal.md for motivation and `specs/api-foundation/spec.md` for the behavior contract. The only hard external constraints are the chosen stack (Java 21, Spring Boot, WebFlux) and the take-home delivery expectations (single command, README sections).

## Goals / Non-Goals

**Goals:**
- A reviewer can clone the repo, run one command, and see a passing test suite on a machine with JDK 21.
- Establish a layered package layout and dependency baseline that A1 (pricing engine) and A2 (order confirmation) can build on without restructuring.
- Keep the skeleton minimal: no domain logic, no persistence, no messaging.

**Non-Goals:**
- Pricing/lever calculation, `POST /orders`, idempotency, outbound effects, ADR content (separate changes).
- Deployment, containers, or CI pipelines.
- Choosing a production database or message broker.

## Decisions

**Build tool: Gradle (Kotlin DSL)**
Gradle over Maven. Rationale: the requested stack; the Gradle wrapper (`gradlew` / `gradlew.bat`) makes the build reproducible on the reviewer's OS without a preinstalled Gradle, and the Spring Boot Gradle plugin wires `bootRun` and dependency management cleanly. Kotlin DSL (`build.gradle.kts`) is the modern default and gives IDE completion and type-checking. Alternative considered: Maven — more universally familiar, but the user chose Gradle.

**Java 21 via the Gradle toolchain**
Configure `java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }` so Gradle uses a JDK 21 toolchain and fails the build when it is unavailable, rather than silently compiling against an older target. Alternative: setting `sourceCompatibility`/`targetCompatibility` to 21 — rejected because they do not prevent linking against newer APIs.

**Spring Boot 3.x with the WebFlux starter**
Use `spring-boot-starter-webflux` (reactive, Netty-based) as required by the take-home. Add `spring-boot-starter-test` for JUnit 5 and `WebTestClient`. Alternative considered: Spring MVC — rejected, the stack is fixed to WebFlux.

**Health endpoint: custom lightweight controller**
Expose `GET /api/v1/health` returning `{"status":"UP"}` from a committed configuration. Alternative considered: Spring Boot Actuator (`/actuator/health`) — richer, but pulls an extra dependency and expands the surface for a skeleton; it can be added later when operational endpoints are actually needed.

**Base package and layered layout**
Base package and Gradle `group` `com.marketplaceb2b`, project name `marketplaceb2b-api`. The source tree is organized in layers: `controller` (HTTP endpoints), `service` (application and domain logic), and `model` (domain types). A layer package is created only once it holds code, so the skeleton ships just `controller` with the health endpoint. Alternative considered: domain/feature packages (`catalog`, `cart`, `pricing`, `orders`) — rejected in favor of the team's familiar layered convention; boundaries can be refined per feature later.

**Configuration in `application.yaml`**
Server port and app metadata come from `application.yaml` so nothing is hardcoded in code, matching the multi-country, config-driven direction of the domain.

**Single command**
Both commands go through the Gradle wrapper. Run the full test suite with `./gradlew clean test` (Windows: `gradlew.bat clean test`); start the service with `./gradlew bootRun` (Windows: `gradlew.bat bootRun`). `test` fails the build on any failing test. Alternative considered: a custom aggregate `verify`-style task — unnecessary while the single `test` task already covers the suite.

## Risks / Trade-offs

- **No actuator/health detail** → a custom endpoint is enough for the skeleton; revisit when real readiness/liveness and metrics are required.
- **Spring Boot version drift** → pin an explicit stable 3.x version on the Spring Boot Gradle plugin so builds are reproducible; upgrade deliberately.
- **Wrapper friction on Windows** → document both `gradlew.bat` (Windows) and `./gradlew` (POSIX); commit both wrappers and the `gradle/wrapper/` files.
- **Empty layer packages may be pruned by tooling or reviewers** → create a layer package (`service`, `model`) only when it holds code, so no placeholder files are needed; the README documents the intended layers.
- **Skeleton could imply finished behavior** → README marks lever order, rounding policy, and consistency model as pending, and the proposal lists A1/A2/A3 as out of scope.

## Open Questions

- Exact Spring Boot 3.x patch version to pin (resolved at implementation by picking the current stable release).
- Whether the base package should be `com.marketplaceb2b` or a team-specific namespace; `com.marketplaceb2b` is assumed unless the user says otherwise.
