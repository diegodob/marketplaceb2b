# Tasks

Assumptions carried from design.md: levers hang off a `ruleset`; family is a `ProductFamily` entity (per tenant); `comboPrice` is the total for the set; gift condition = quantity and/or amount; tiers strictly increasing; `BigDecimal` for money/percentages; declarative `jakarta.validation` constraints; package `com.marketplaceb2b.model`.

## 1. Enums and scale discount

- [ ] 1.1 Create `DiscountTargetType` (`SKU`, `FAMILY`); verify a closed-set test
- [ ] 1.2 Create `ScaleTier` (`fromQty` positive, `percentage` `0`–`100`) and `ScaleDiscount` (target `productId`/`familyId` + non-empty tiers, `@AssertTrue` target consistency and strictly increasing tiers); verify a `Validator` reports a non-positive start, out-of-range percentage, empty tiers, an inconsistent target and non-increasing tiers

## 2. Combo

- [ ] 2.1 Create `ComboItem` (positive `minQty`) and `Combo` (non-empty items, non-negative `comboPrice` = total for the set); verify a `Validator` reports empty items, non-positive `minQty` and a negative price

## 3. Gift and product family

- [ ] 3.1 Create `GiftCondition` (optional `productId`/`familyId`, optional positive `minQty`, optional non-negative `minAmount`, `@AssertTrue` well-formedness) and `Gift` (condition + `giftProductId` + positive `giftQty`); verify a `Validator` reports a missing threshold, a quantity threshold without a target, both targets, and a non-positive gift quantity
- [ ] 3.2 Create `ProductFamily(id, tenantId, name)` and change `Product` to reference `familyId`; verify the architecture test still passes

## 4. Architecture and build

- [ ] 4.1 Add the lever records and `ProductFamily` to `ModelArchitectureTest`; verify `comboPrice`/`percentage` are `BigDecimal`
- [ ] 4.2 Run `gradlew.bat clean test` and confirm the full suite passes with exit code `0`

## Workflow follow-up

- After the maintainer merges the PR, archive the change (`openspec archive pricing-levers`).
- A follow-up persistence change adds the `product_family` table and `product.family_id`.
