# Spec Delta

## MODIFIED Requirements

### Requirement: README

The repository SHALL include a README that states the technology stack (Java 21, Spring Boot, WebFlux, Gradle), the exact commands to run the service and its tests, and the design-notes sections for the lever application order, the rounding policy and the consistency model, kept up to date with the implemented design.

#### Scenario: README documents stack, run, test, and design notes

- **WHEN** a reader opens the README
- **THEN** it names the technology stack, states the run command and the test command, and contains the lever-order, rounding-policy and consistency-model sections reflecting the current design
