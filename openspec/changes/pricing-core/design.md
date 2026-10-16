# Design

## Context

`model` has the domain types; `persistence` has the schema and the lever repositories. Nothing computes a price. This change adds the baseline `order-pricing` capability: the breakdown with gross, tax and total, without levers. See proposal.md and `specs/order-pricing/spec.md`.

## Goals / Non-Goals

**Goals:**
- One use case, `price(cart)`, returning the breakdown with exact money and per-line rounding.
- A breakdown shape the later slices (levers, quote, credit) extend without rework.

**Non-Goals:**
- Levers (combo/scale/gift), quote creation, the credit check, HTTP.

## Decisions

**Entry point.** `OrderPricingService.price(cart)` returns a `PriceBreakdown` (no persistence; quote creation is a later change).

**Cart DTO.** `Cart(tenantId, customerId, paymentMethodId, lines)`, each `CartLine(productId, qty)`. This baseline uses `tenantId` and the lines; `customerId`/`paymentMethodId` are carried for the later slices.

**Breakdown DTO.** `PriceBreakdown(tenantId, rulesetId, currency, lines, discounts, gifts, grossSubtotal, totalDiscounts, totalTaxableBase, totalTax, total)`:
- `LineBreakdown(productId, qty, unitPrice, gross, taxableBase, taxRate, tax)`;
- `DiscountBreakdown(origin, detail, amount)` — `origin` is `DiscountOrigin` (`COMBO`, `SCALE`), one entry per applied lever occurrence; empty here;
- `GiftBreakdown(productId, qty, taxable, tax)` — empty here.

The `discounts`/`gifts` lists are present but empty, so the lever slices fill them without changing the contract. The credit-fits field is intentionally **absent** (a separate change adds it).

**Current ruleset.** Resolve the row with the greatest `effective_from` not in the future for the tenant; fail with an explicit error when there is none.

**Reads.** A read for the current ruleset; a read of the cart products' `product_price` rows for the ruleset (unit price and tax category); a read of the ruleset's `tax_rate` rows. A product without a price, or a category without a rate, fails explicitly — never 0.

**Rounding via a `Money` value object.** To avoid raw, error-prone `BigDecimal` arithmetic with scattered `setScale` calls, money is modelled by a `Money` record (`amount`, `scale`, `roundingMode`) built from the ruleset. Every operation on it returns a new `Money` already rounded to the currency scale with the ruleset mode — `times(qty)`, `times(factor)`, `percentage(rate)` (the tax = amount × rate / 100) and `plus(...)` (which rejects mixing different policies). The engine works in `Money` and unwraps to `BigDecimal` only at the DTO boundary, so the scale/rounding can never be forgotten. All money stays `BigDecimal` underneath.

**Package.** The services live in `com.marketplaceb2b.service`; the request/response DTOs (`Cart`, the breakdowns, `OrderConfirmation`) in `com.marketplaceb2b.service.dto`; the exceptions in `com.marketplaceb2b.service.exception`. `Money` is a domain value object, so it lives in `com.marketplaceb2b.model` alongside the entities.

## Risks / Trade-offs

- **DTO extensibility** → `discounts`/`gifts` are lists from the start; the lever slices only fill them. The credit field is added by its own change (a small contract change there).
- **N reads per cart** → fine for the small carts here; a batch read by product ids is used.
- **Rounding policy duplication** → centralised in `Money`; the model `RoundingMode` maps to `java.math.RoundingMode` in one place. The quote/confirmation reuse the stored, already-rounded values.

## Open Questions

- None blocking. The credit-fits field is deferred to the `credit-fits` change.
