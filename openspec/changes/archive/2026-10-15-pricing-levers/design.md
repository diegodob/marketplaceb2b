# Design

## Context

`model` already holds the immutable domain types for the order flow. This change adds the commercial-lever types and turns a product's family into a reference to a tenant-scoped entity. See proposal.md for motivation and `specs/pricing-levers/spec.md` for the behaviour contract. Scope is the domain model plus a small persistence change for product families.

## Goals / Non-Goals

**Goals:**
- Immutable, validated types for scale discounts, combos and gifts.
- A tenant-scoped `ProductFamily` entity so families are referable, not free text.
- Shapes ready for a later pricing engine.

**Non-Goals:**
- The lever tables (`scale_discount`, `scale_tier`, `combo`, `combo_item`, `gift`, `gift_condition`) and the pricing engine.
- The order in which levers are applied.

## Decisions

**Family is an entity, per tenant.** A `ProductFamily(id, tenantId, name)` belongs to a tenant, with `name` unique per tenant; `Product.familyId` and the levers' family target reference it by id, replacing the free-text `family`. The schema ships in this change (see the migration decision below).

**Scale discount = target + cumulative, increasing tiers.** A `ScaleDiscount` has a `DiscountTargetType` (`SKU` or `FAMILY`), a `productId` or a `familyId`, and a non-empty list of `ScaleTier(fromQty, percentage)`. The tiers must be strictly increasing by `fromQty`; that check is an `@AssertTrue` on the record, so it runs with every other constraint when a validator is invoked (at load/validation time), consistent with the rest of the flows. A scale targets **exactly one product or one family** (a deliberate simplification for v1); a scale spanning several targets is modelled as several scale discounts.

**Combo = items + a total price.** A `Combo` has a `name`, a non-empty list of `ComboItem(productId, minQty)` and a non-negative `comboPrice` that is the **total for the set**. `minQty` is a floor (the take-home's "cantidades mínimas"): the minimum quantity of each item needed to reach the combo. Engine rule for later: units **above** the minimum are charged at the regular price — with a minimum of 6 and 8 bought, the 6 go in the combo and the extra 2 are paid.

**Gift = condition (quantity and/or amount) + gift product/quantity.** A `GiftCondition` carries an optional `productId`/`familyId` target, an optional positive `minQty` and an optional non-negative `minAmount`. It is well-formed when it has at least one threshold, a quantity threshold names exactly one target, and the two targets are never both set. Either threshold may be absent on its own (`minQty` null, or `minAmount` null) as long as one is present; when both are present the trigger is **AND** (a disjunction is expressed by defining two separate gift conditions).

**Cross-field rules via `@AssertTrue`; `BigDecimal` for money/percentages; enums for closed sets; immutable records; `Long id` nullable; package `com.marketplaceb2b.model`.** Consistent with the existing model; validation is declarative, not thrown in constructors. List components (`ScaleDiscount.tiers`, `Combo.items`) must be non-empty and their elements non-null (`List<@NotNull ...>`), so a null element is a validation error rather than being silently skipped by the cascade.

**Additive migration for the family.** `V1__schema.sql` is already released, so the family is added through a new `V3__product_family.sql` rather than rewriting `V1`: it creates `product_family` (`UNIQUE (tenant_id, name)`), adds a nullable `product.family_id` with an index, adds the FK to `product_family`, and drops the now-unused `product.family` column. A `ProductFamilyRepository` gives the new entity its data-access path.

## Risks / Trade-offs

- **Scale targets a single product or family** → a multi-target scale becomes several scale discounts. Accepted for v1. *(for the ADR)*
- **Combo minimum is a floor** → the units above the minimum are charged at the regular price; the engine must split the line. *(for the ADR)*
- **`V3` drops `product.family`** → safe here because nothing populated or reads it (the seed sets no family). A real deployment would backfill first.
- **Gift AND semantics** → the model captures the shape; the engine applies the AND when both thresholds are present.
- **No engine / lever tables** → the lever definitions are not persisted yet; a later change adds their tables and the engine.

## Open Questions

- None blocking.
