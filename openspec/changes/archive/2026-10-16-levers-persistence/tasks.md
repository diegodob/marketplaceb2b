# Tasks

Assumptions carried from design.md: additive migration `V4__levers.sql`, levers tied to `ruleset_id`, tier ordering app-validated (not DB), repositories load aggregates over `PersistenceAccess`.

## 1. Schema

- [x] 1.1 Author `V4__levers.sql`: `scale_discount`, `scale_tier`, `combo`, `combo_item`, `gift_condition`, `gift` with the `CHECK`s and explicit FK indexes; verify the migration applies cleanly
- [x] 1.2 Add `V5__levers_seed.sql`: a product family, family assignments on the seeded products, and the example levers; verify it applies and is idempotent across restarts

## 2. Repositories

- [x] 2.1 `ScaleDiscountRepository.findByRuleset(rulesetId)` loading each scale with its tiers; verify a round-trip test returns the tiers in order
- [x] 2.2 `ComboRepository.findByRuleset(rulesetId)` loading each combo with its items; verify a round-trip test
- [x] 2.3 `GiftRepository.findByRuleset(rulesetId)` loading each gift with its condition; verify a round-trip test

## 3. Tests

- [x] 3.1 Add constraint tests: scale target inconsistent with the type rejected; `from_qty <= 0` and out-of-range `percentage` rejected; negative `combo_price`, `min_qty <= 0` and a duplicate `(combo_id, product_id)` rejected; a gift condition with no threshold, a quantity threshold without target, both targets or a negative amount rejected; `gift_qty <= 0` rejected
- [x] 3.2 Add the lever tables to `ForeignKeyIndexTest` and verify every foreign key has a leading index

## 4. Build verification

- [x] 4.1 Run `gradlew.bat clean test` and confirm the full suite passes with exit code `0`

## Workflow follow-up

- After the maintainer merges the PR, archive the change (`openspec archive levers-persistence`).
