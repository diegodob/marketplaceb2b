# Proposal

## Why

The README still marks the take-home's required design sections — lever application order, rounding policy and consistency model — as TBD, and its Scope and Structure no longer match the code. The `api-foundation` spec also still requires the README to keep those sections "marked as not yet defined", which no longer holds. This change brings both the documentation and that requirement in line with the current design.

## What Changes

- Fill **Lever application order** (the strategy pipeline, the priority order combo → scale → gift → tax, and unit-consumption exclusivity).
- Fill **Rounding policy** (per-line rounding, `DECIMAL` money, the `Money` value object).
- Fill **Consistency model** (immutable quote, single-transaction confirmation, transactional outbox).
- Update **Scope** and **Structure** and the migrations list, and the combo simplification (priority, not size).
- Relax the `api-foundation` README requirement so the design-notes sections reflect the current design instead of being "marked as not yet defined".

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `api-foundation`: the README requirement no longer demands the design-notes sections be "marked as not yet defined"; it requires them to reflect the current design.

## Impact

- `README.md` and the `api-foundation` spec.
