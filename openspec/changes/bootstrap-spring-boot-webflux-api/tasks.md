# Tasks

Assumptions carried from design.md: base package `com.marketplaceb2b`, Gradle `group`/name `com.marketplaceb2b` / `marketplaceb2b-api`, Kotlin DSL, Spring Boot plugin at the current stable 3.x at implementation time.

## 1. Build and toolchain

- [x] 1.1 Create `settings.gradle.kts` and `build.gradle.kts` applying the Spring Boot 3.x and dependency-management plugins, with `group`/name `com.marketplaceb2b` / `marketplaceb2b-api` and a Java 21 toolchain; verify with `gradlew.bat dependencies` that resolution uses the Java 21 toolchain
- [x] 1.2 Declare dependencies `spring-boot-starter-webflux` and `spring-boot-starter-test` (test); verify resolution succeeds with `gradlew.bat -q dependencies --configuration testRuntimeClasspath`
- [x] 1.3 Generate and commit the Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/`); verify `gradlew.bat -v` reports the pinned Gradle version
- [x] 1.4 Add `src/main/resources/application.yaml` with the server port and app name; verify the app reads it (no hardcoded port in Java)

## 2. Source layout and bootstrap

- [x] 2.1 Create the Spring Boot main class under `com.marketplaceb2b`; verify the application starts with `gradlew.bat bootRun` and stops cleanly
- [x] 2.2 Create the `controller` layer package and place the health endpoint handler there; verify the health test still passes and no `package-info.java` placeholders exist

## 3. Health endpoint

- [x] 3.1 Implement `GET /api/v1/health` returning HTTP `200` and `{"status":"UP"}`; verify by running the app and requesting the endpoint
- [x] 3.2 Add a `WebTestClient` test asserting HTTP `200` and the JSON body; verify the test passes with `gradlew.bat test --tests "*Health*"`

## 4. Documentation

- [x] 4.1 Write `README.md` that names the technology stack (Java 21, Spring Boot, WebFlux, Gradle) and gives the exact run command (`gradlew.bat bootRun` / `./gradlew bootRun`) and test command (`gradlew.bat clean test` / `./gradlew clean test`), plus the take-home sections (lever application order, rounding policy, consistency model) marked as pending; verify the documented commands run as written
- [x] 4.2 Confirm the README marks A1/A2/A3 as out of scope for this change; verify no section claims behavior that isn't implemented

## 5. Integration verification

- [x] 5.1 Run the single build-and-test command (`gradlew.bat clean test`) on a clean checkout and confirm it compiles, runs the full suite, and exits `0` with all tests passing
- [x] 5.2 Confirm the four `api-foundation` requirements in `specs/api-foundation/spec.md` all have a passing or observable check (startup, health 200, controller layer package present, command exits non-zero on a deliberately failing test)

## Workflow follow-up

- Archive the change after review and once the skeleton is confirmed on JDK 21.
