# Tasks

Documentation-only change (`skip_specs: true`); no code, schema or behaviour changes.

## 1. Refresh the README

- [x] 1.1 Update the description, Stack (add H2 in PostgreSQL mode, Flyway, JDBC) and the package structure (`controller`, `service`, `model`, `persistence`, `persistence.repository`); verify each statement matches the code
- [x] 1.2 Update Scope: list what is implemented (health; immutable domain model; Flyway schema + seed + `product_family`; idempotent order confirmation; lever domain types) and what is pending (pricing engine + lever tables; outbox relay; HTTP endpoints; ADR; front); verify no section claims behaviour that is not implemented
- [x] 1.3 Add the "Known simplifications & rules (for the ADR)" section (scale single target; combo minimum with the extra units charged; gift AND thresholds) and keep the pending design-note headings
- [x] 1.4 Review the README end to end against the repository and confirm the commands (`gradlew.bat clean test`, `gradlew.bat bootRun`) run as written

## Workflow follow-up

- Merge this change after `pricing-levers` (PR #8) so the README describes the merged state.
- After the maintainer merges the PR, archive the change (`openspec archive refresh-readme`).
