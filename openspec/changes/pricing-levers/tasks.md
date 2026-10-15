# Tasks

Assumptions carried from design.md: levers hang off a `ruleset`; `BigDecimal` for money/percentages; declarative `jakarta.validation` constraints (not thrown in constructors); immutable records; package `com.marketplaceb2b.model`.

## 1. Enums and scale discount

- [x] 1.1 Create `DiscountTargetType` (`SKU`, `FAMILY`); verify `RoundingMode`-style closed-set test
- [x] 1.2 Create `ScaleTier` (`fromQty` positive, `percentage` `0`–`100`) and `ScaleDiscount` (target + tiers, `@AssertTrue` target consistency, non-empty tiers); verify a `Validator` reports non-positive `fromQty`, out-of-range percentage, empty tiers and an inconsistent target

## 2. Combo

- [x] 2.1 Create `ComboItem` (positive `minQty`) and `Combo` (non-empty items, non-negative `comboPrice`); verify a `Validator` reports empty items, non-positive `minQty` and a negative price

## 3. Gift

- [x] 3.1 Create `GiftConditionType` (`PRODUCT_QTY`, `FAMILY_QTY`), `GiftCondition` (`@AssertTrue` type consistency) and `Gift` (condition + `giftProductId` + positive `giftQty`); verify a `Validator` reports a non-positive gift quantity and an inconsistent condition

## 4. Architecture and build

- [x] 4.1 Add the lever records to `ModelArchitectureTest` (`allTypesAreRecords`, no float/double); verify `comboPrice`/`percentage` are `BigDecimal`
- [x] 4.2 Run `gradlew.bat clean test` and confirm the full suite passes with exit code `0`

## Workflow follow-up

- After the maintainer merges the PR, archive the change (`openspec archive pricing-levers`).
