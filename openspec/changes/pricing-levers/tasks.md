# Tasks

Assumptions carried from design.md: levers hang off a `ruleset`; family is a `ProductFamily` entity (per tenant); `comboPrice` is the total for the set; gift condition = quantity and/or amount; tiers strictly increasing; `BigDecimal` for money/percentages; declarative `jakarta.validation` constraints; package `com.marketplaceb2b.model`.

## 1. Enums and scale discount

- [x] 1.1 Create `DiscountTargetType` (`SKU`, `FAMILY`); verify a closed-set test
- [x] 1.2 Create `ScaleTier` (`fromQty` positive, `percentage` `0`–`100`) and `ScaleDiscount` (target `productId`/`familyId` + non-empty tiers, `@AssertTrue` target consistency and strictly increasing tiers); verify a `Validator` reports a non-positive start, out-of-range percentage, empty tiers, an inconsistent target and non-increasing tiers

## 2. Combo

- [x] 2.1 Create `ComboItem` (positive `minQty`) and `Combo` (non-empty items, non-negative `comboPrice` = total for the set); verify a `Validator` reports empty items, non-positive `minQty` and a negative price

## 3. Gift and product family

- [x] 3.1 Create `GiftCondition` (optional `productId`/`familyId`, optional positive `minQty`, optional non-negative `minAmount`, `@AssertTrue` well-formedness) and `Gift` (condition + `giftProductId` + positive `giftQty`); verify a `Validator` reports a missing threshold, a quantity threshold without a target, both targets, and a non-positive gift quantity
- [x] 3.2 Create `ProductFamily(id, tenantId, name)` and change `Product` to reference `familyId`; verify the architecture test still passes

## 4. Architecture and build

- [x] 4.1 Add the lever records and `ProductFamily` to `ModelArchitectureTest`; verify `comboPrice`/`percentage` are `BigDecimal`
- [x] 4.2 Run `gradlew.bat clean test` and confirm the full suite passes with exit code `0`

## 5. Product family persistence

- [x] 5.1 Author `V3__product_family.sql`: create `product_family` (`UNIQUE (tenant_id, name)`), add `product.family_id` (indexed FK to `product_family`) and drop `product.family`; verify the migration applies cleanly
- [x] 5.2 Add `ProductFamilyRepository` (`find`, `insert`); verify a round-trip test
- [x] 5.3 Add tests asserting rejection of a duplicate family name within a tenant, acceptance of the same name across tenants, and rejection of a product with an unknown `family_id`
- [x] 5.4 Run `gradlew.bat clean test` and confirm the full suite passes with exit code `0`

## Workflow follow-up

- After the maintainer merges the PR, archive the change (`openspec archive pricing-levers`).
