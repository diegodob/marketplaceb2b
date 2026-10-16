# Proposal

## Why

Data access currently sits across two nested packages: `com.marketplaceb2b.persistence` (the `PersistenceAccess` JDBC wrapper and the append-only guard) and `com.marketplaceb2b.persistence.repository` (the repositories). The extra nesting adds no value and is inconsistent with the flat, top-level layout the rest of the project now uses (`model`, `dto`).

## What Changes

- Move `PersistenceAccess` and every `*Repository` into a single top-level **`com.marketplaceb2b.repository`** package.
- Remove `com.marketplaceb2b.persistence`.
- Update the imports in the services and tests that use them.
- **No behaviour change** — a package move only (pure refactor).

## Capabilities

### New Capabilities
- None — this change is a refactor with no spec-level behaviour change and opts out with `skip_specs: true`.

### Modified Capabilities
- None.

## Impact

- Package move and import updates only. No schema, dependency, API or runtime behaviour change.
