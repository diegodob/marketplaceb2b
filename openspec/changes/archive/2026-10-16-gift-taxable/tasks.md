# Tasks

## 1. Gift taxable flag

- [x] 1.1 Add a `boolean taxable` component to `Gift`, map it in `GiftRepository`, and update the gift constructions in `PricingLeversTest`; verify `gradlew.bat test --tests "*PricingLeversTest"` passes
- [x] 1.2 Add `V6__gift_taxable.sql` (`ALTER TABLE gift ADD COLUMN taxable BOOLEAN NOT NULL DEFAULT FALSE`) and verify the migration applies (the app context starts)
- [x] 1.3 Add a test asserting the seeded gifts are non-taxable and a stored taxable gift reads back as taxable; verify it passes
- [x] 1.4 Run `gradlew.bat clean test` and confirm the full suite passes with exit code `0`

## Workflow follow-up

- After the maintainer merges the PR, archive the change (`openspec archive gift-taxable`).
