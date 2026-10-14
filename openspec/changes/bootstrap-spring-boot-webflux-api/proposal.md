# Proposal

## Why

The take-home is a multi-tenant B2B marketplace order service, and the chosen stack is Java 21 with Spring Boot / WebFlux. Today the repository holds only the OpenSpec scaffold — there is no application source code, so there is nothing to build, run, or test. A minimal, buildable service skeleton gives a shared foundation (build, runtime, test harness, one run command) before any domain capability is added.

## What Changes

- Add a Gradle project (Kotlin DSL) targeting **Java 21** with **Spring Boot 3.x** and the **WebFlux** reactive stack (`spring-boot-starter-webflux`).
- Add the application bootstrap (main class) and a base `application.yaml` configuration.
- Add a layered package structure (`controller`, `service`, `model`) so later capabilities have a home; only the `controller` layer and the bootstrap class contain code in this change.
- Add one reactive HTTP endpoint for health/readiness that returns a JSON status payload, with a `WebTestClient`-based test asserting `200` and the payload shape.
- Add the test harness (JUnit 5 + `spring-boot-starter-test`) and a single documented Gradle command that builds and runs all tests.
- Add a README that documents the technology stack and the exact commands to run the service and its tests, plus the placeholders the take-home asks for (lever application order, rounding policy, consistency model), each marked as not yet defined.
- Out of scope (separate future changes): the A1 pricing/lever engine, the A2 idempotent `POST /orders` confirmation, the A3 ADR, and the optional Angular/Flutter front end.

## Capabilities

### New Capabilities
- `api-foundation`: the service's runtime foundation — a bootable Java 21 Spring Boot WebFlux application that exposes a reactive HTTP endpoint and ships with a single command to build and test it.

### Modified Capabilities
- None.

## Impact

- Introduces the first application source tree in this repository (currently empty outside `openspec/`).
- Adds build tooling and dependency management: Gradle with the Spring Boot plugin, WebFlux, JUnit 5, `WebTestClient`, and a Java 21 toolchain requirement.
- Establishes the build/run/test entry point and a README; no existing code, APIs, or specs are affected.
