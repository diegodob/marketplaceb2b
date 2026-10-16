# Proposal

## Why

The README still describes the repository as a bare "service skeleton" and lists only the health endpoint as implemented, which no longer matches the codebase: the domain model, the Flyway schema and seed, the repositories and the idempotent order confirmation all exist now. The README should tell a reviewer what is really here, and record the accepted lever simplifications for the ADR.

## What Changes

- Rewrite the README to reflect reality: description, stack (add H2, Flyway, JDBC), package structure, and commands.
- Update **Scope**: what is implemented vs what is still pending.
- Add a **"Known simplifications & rules (for the ADR)"** section: scale targets a single product or family; combo minimum with extra units charged at the regular price; gift thresholds combine with AND.
- Documentation only: no code, schema or behaviour changes.

## Capabilities

### New Capabilities
- None — this change is documentation only and opts out of spec deltas (`skip_specs: true`).

### Modified Capabilities
- None.

## Impact

- `README.md` only. No source, schema or dependency changes.
