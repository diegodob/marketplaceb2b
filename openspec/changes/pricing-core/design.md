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

**Rounding.** `gross = qty × unit_price`; `taxableBase = gross` (no discounts yet); `tax = taxableBase × rate`; each line's `taxableBase` and `tax` are set to the ruleset currency scale with the ruleset rounding mode (mapping the model `RoundingMode` to `java.math.RoundingMode`); the total is the sum of the rounded line values. All money is `BigDecimal`.

**Package.** The service and DTOs live in `com.marketplaceb2b.service` (DTOs may go in a `pricing` subpackage).

## Risks / Trade-offs

- **DTO extensibility** → `discounts`/`gifts` are lists from the start; the lever slices only fill them. The credit field is added by its own change (a small contract change there).
- **N reads per cart** → fine for the small carts here; a batch read by product ids is used.
- **Rounding helper duplicated** → a single mapping from `RoundingMode` to `java.math.RoundingMode` in the service; the quote/confirmation reuse the stored, already-rounded values.

## Open Questions

- None blocking. The credit-fits field is deferred to the `credit-fits` change.
