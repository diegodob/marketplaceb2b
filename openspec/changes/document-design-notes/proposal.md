# Proposal

## Why

The README still marks the take-home's required design sections — lever application order, rounding policy and consistency model — as TBD, and its Scope and Structure no longer match the code (the pricing engine baseline exists, and the packages were reorganised). This change completes that documentation so the repository reflects the current design.

## What Changes

- Fill **Lever application order** (the strategy pipeline, the priority order combo → scale → gift → tax, and unit-consumption exclusivity).
- Fill **Rounding policy** (per-line rounding, `DECIMAL` money, the `Money` value object).
- Fill **Consistency model** (immutable quote, single-transaction confirmation, transactional outbox).
- Update **Scope** (the pricing engine baseline is implemented) and **Structure** (`dto`, `repository`), the migrations list, and the combo simplification (priority, not size).
- Documentation only; no behaviour change.

## Capabilities

### New Capabilities
- None — documentation only, opts out with `skip_specs: true`.

### Modified Capabilities
- None.

## Impact

- `README.md` only.
