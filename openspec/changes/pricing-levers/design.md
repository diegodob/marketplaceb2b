# Design

## Context

`model` already holds the immutable domain types for the order flow (products, prices, taxes, quotes, orders). This change adds the commercial-lever types. See proposal.md for motivation and `specs/pricing-levers/spec.md` for the behaviour contract. The types must be persistence-agnostic and hang off a `ruleset`, like prices and taxes.

## Goals / Non-Goals

**Goals:**
- Immutable, validated types for scale discounts, combos and gifts.
- A shape ready for a later pricing engine to consume.
- Fast plain-JUnit tests (a `Validator`, no Spring context).

**Non-Goals:**
- Tables/migrations, repositories.
- The calculation engine and the order in which levers are applied.
- Applying levers to a quote.

## Decisions

**Levers are versioned by `ruleset`.** Each lever carries a `rulesetId`, exactly as `product_price` and `tax_rate` do, so a lever definition belongs to one commercial-rules version.

**Scale discount = target + cumulative tiers.** A `ScaleDiscount` has a `DiscountTargetType` (`SKU` or `FAMILY`), a `productId` (SKU) or a `family` (FAMILY), and a non-empty list of `ScaleTier(fromQty, percentage)`. The tiers are ordered by `fromQty`; the engine will apply them per tramo. Alternative considered: a flat list of `(minQty, percentage)` applied as a single best-match — rejected; the take-home says scales are cumulative by tramos.

**Cross-field consistency via `@AssertTrue`.** A `ScaleDiscount` exposes `isTargetConsistent()` (SKU ⇒ productId set and family null; FAMILY ⇒ the reverse); a `GiftCondition` does the same. This keeps the rule declarative and validator-checked, as with `CreditAccount`'s balance rule.

**Combo = items + price.** A `Combo` has a `name`, a non-empty list of `ComboItem(productId, minQty)` and a non-negative `comboPrice`. Whether `comboPrice` is a total for the set or a per-unit price is deferred (Open Questions).

**Gift = condition + gift product/quantity.** A `Gift` has a `GiftCondition` and a `giftProductId`/`giftQty`. The condition is modelled minimally as a type (`PRODUCT_QTY`/`FAMILY_QTY`), a `productId`/`family` and a `minQty`; richer conditions are deferred (Open Questions).

**`BigDecimal` for money and percentages; enums for closed sets; immutable records; `Long id` nullable until persisted.** Consistent with the existing model; validation is declarative (`jakarta.validation`), not thrown in the constructor.

**Package `com.marketplaceb2b.model`.** The lever types join the existing model package so the engine change finds one vocabulary.

## Risks / Trade-offs

- **The lever semantics are richer than the model** → the model captures the *shape*, not the calculation; the engine change will add the application rules. Accepted, by scope.
- **`comboPrice`/`family` ambiguity** → carried as open questions; the types can be refined with a migration-free change since nothing is persisted yet.
- **No persistence** → the types are not stored; the engine change adds the tables.

## Open Questions

- Is `comboPrice` a total for the set, or a per-unit price?
- Is the gift condition limited to "at least N of a product/family", or does it also cover "minimum total amount"?
- Is `family` a free-text string (as in `product.family`) or should it become its own entity?
- Must the tiers be strictly increasing in `fromQty`, and is that validated in the model or by the engine?
