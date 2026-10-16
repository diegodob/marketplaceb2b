# Tasks

## 1. Priority on the levers

- [ ] 1.1 Add `V7__lever_priority.sql` (`ALTER TABLE scale_discount|combo|gift ADD COLUMN priority INT NOT NULL DEFAULT 0`) plus a backfill of example priorities; verify the migration applies
- [ ] 1.2 Add an `int priority` component to `ScaleDiscount`, `Combo` and `Gift` and map it in `ScaleDiscountRepository`, `ComboRepository` and `GiftRepository`; update the constructions in the model and repository tests; verify the tests pass
- [ ] 1.3 Verify a lever read back from the seed carries its priority (a repository test) and that `gradlew.bat clean test` passes with exit code `0`

## Workflow follow-up

- After the maintainer merges the PR, archive the change (`openspec archive lever-priority`).
