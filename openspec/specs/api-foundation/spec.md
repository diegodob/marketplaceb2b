# api-foundation Specification

## Purpose
Provides the runtime foundation for the B2B marketplace API: a bootable Java 21 Spring Boot WebFlux service that exposes a reactive HTTP endpoint and can be built and tested with a single command, so later domain capabilities have a working place to land.

## Requirements

### Requirement: Java 21 WebFlux service foundation

The service SHALL run as a Spring Boot application on the WebFlux reactive stack targeting Java 21, and SHALL start using only its committed configuration.

#### Scenario: Application starts

- **WHEN** the service is started with the documented run command
- **THEN** it boots without errors and listens for HTTP requests on the configured port

#### Scenario: Java 21 toolchain required

- **WHEN** the project is compiled with a JDK older than 21
- **THEN** the build fails rather than silently producing a build for an older target

### Requirement: Health endpoint

The service SHALL expose an HTTP endpoint that reports service status.

#### Scenario: Health check succeeds

- **WHEN** a client issues `GET` to the health endpoint
- **THEN** the service responds with HTTP `200` and a JSON body indicating the service is up

### Requirement: Layered source layout

The application source tree SHALL be organized in layers: `controller` for HTTP endpoints, `service` for application and domain logic, and `model` for domain types. A layer package SHALL exist only once it contains code.

#### Scenario: Controller layer exists

- **WHEN** the source tree is inspected
- **THEN** the `com.marketplaceb2b.controller` package exists and contains the health endpoint handler

### Requirement: Single build-and-test command

The project SHALL provide one documented command that compiles the code and runs the complete automated test suite, and that fails when any test fails.

#### Scenario: Command runs the suite

- **WHEN** a developer runs the documented command on a clean checkout
- **THEN** the project compiles, the full automated test suite runs, and the command exits with a success code when all tests pass

#### Scenario: Failing test fails the command

- **WHEN** any test in the suite fails
- **THEN** the command exits with a non-zero status

### Requirement: README

The repository SHALL include a README that states the technology stack (Java 21, Spring Boot, WebFlux, Gradle), the exact commands to run the service and its tests, and the design-notes sections for the lever application order, the rounding policy and the consistency model, kept up to date with the implemented design.

#### Scenario: README documents stack, run, and test

- **WHEN** a reader opens the README
- **THEN** it names the technology stack, states the run command and the test command, and contains the lever-order, rounding-policy and consistency-model sections reflecting the current design

### Requirement: Automated health test

The build SHALL include an automated test that asserts the health endpoint behavior.

#### Scenario: Health test runs in the suite

- **WHEN** the automated test suite runs
- **THEN** a test exercises the health endpoint and asserts HTTP `200` with the expected JSON body
