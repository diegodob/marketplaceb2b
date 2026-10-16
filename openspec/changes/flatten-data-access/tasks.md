# Tasks

Refactor only (`skip_specs: true`); no behaviour change.

## 1. Flatten the data-access package

- [x] 1.1 Move `PersistenceAccess` and every `*Repository` from `com.marketplaceb2b.persistence(.repository)` to `com.marketplaceb2b.repository` (update their package declarations and remove the inner self-imports); verify the main sources compile
- [x] 1.2 Update the imports in the services and tests that reference the moved types; verify no reference to `com.marketplaceb2b.persistence` remains
- [x] 1.3 Remove the now-empty `com.marketplaceb2b.persistence` directory; verify the project still compiles
- [x] 1.4 Run `gradlew.bat clean test` and confirm the full suite passes with exit code `0`

## Workflow follow-up

- After the maintainer merges the PR, archive the change (`openspec archive flatten-data-access`).
