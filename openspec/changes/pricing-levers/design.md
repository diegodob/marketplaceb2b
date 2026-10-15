# Design

## Context

`model` already holds the immutable domain types for the order flow. This change adds the commercial-lever types and turns a product's family into a reference to a tenant-scoped entity. See proposal.md for motivation and `specs/pricing-levers/spec.md` for the behaviour contract. Scope is the domain model only.

## Goals / Non-Goals

**Goals:**
- Immutable, validated types for scale discounts, combos and gifts.
- A tenant-scoped `ProductFamily` entity so families are referable, not free text.
- Shapes ready for a later pricing engine.

**Non-Goals:**
- Tables/migrations, repositories (the schema change follows in a persistence change).
- The calculation engine and the order of application.

## Decisions

**Family is an entity, per tenant.** A `ProductFamily(id, tenantId, name)` belongs to a tenant; `Product.familyId` and the levers' family target reference it by id, replacing the free-text `family`. The persistence change that adds the `product_family` table and `product.family_id` follows separately; the domain is corrected now.

**Scale discount = target + cumulative, increasing tiers.** A `ScaleDiscount` has a `DiscountTargetType` (`SKU` or `FAMILY`), a `productId` or a `familyId`, and a non-empty list of `ScaleTier(fromQty, percentage)`. The tiers must be strictly increasing by `fromQty`; that check is an `@AssertTrue` on the record, so it runs with every other constraint when a validator is invoked (at load/validation time), consistent with the rest of the flows.

**Combo = items + a total price.** A `Combo` has a `name`, a non-empty list of `ComboItem(productId, minQty)` and a non-negative `comboPrice` that is the **total for the set**.

**Gift = condition (quantity and/or amount) + gift product/quantity.** A `GiftCondition` carries an optional `productId`/`familyId` target, an optional positive `minQty` and an optional non-negative `minAmount`. It is well-formed when it has at least one threshold, a quantity threshold names exactly one target, and the two targets are never both set. Whether both thresholds combine as AND or OR is deferred to the engine.

**Cross-field rules via `@AssertTrue`; `BigDecimal` for money/percentages; enums for closed sets; immutable records; `Long id` nullable; package `com.marketplaceb2b.model`.** Consistent with the existing model; validation is declarative, not thrown in constructors.

## Risks / Trade-offs

- **Domain ahead of schema** → `Product.familyId` and `ProductFamily` have no table yet; the persistence change will add `product_family` and `product.family_id`. Accepted by scope.
- **Gift AND/OR semantics undefined** → the model captures the shape; the engine fixes the semantics.
- **No engine** → the types are definitions only; nothing consumes them yet.

## Open Questions

- When a gift condition carries both a quantity and an amount threshold, is the trigger an AND or an OR?
- Should `ProductFamily.name` be unique per tenant (like `product` SKU is)?
