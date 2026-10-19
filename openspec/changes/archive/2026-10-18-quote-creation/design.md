# Design

## Context

See proposal.md — Why. `OrderPricingService.price(cart)` returns a `PriceBreakdown` but persists nothing. The `quote` (immutable header) and `quote_line` (frozen lines) tables exist, but the application never writes them; `quote_line` has `UNIQUE (quote_id, product_id)`. The B1 persistence (`quote_line_discount`) was recorded in the pricing-combo design; the gift persistence was left as an open question, since a gift line for a product already paid would break that uniqueness.

## Goals / Non-Goals

**Goals:**
- Persist the frozen quote (lines, discounts and gifts) so the confirmation reads exactly what was quoted.
- Make a cross-tenant customer or quote impossible at the database.

**Non-Goals:**
- The order-confirmation endpoint (A2) and the outbox relay.

## Decisions

**The breakdown maps to four tables.** The header maps to `quote`; the **paid** lines to `quote_line`; each applied discount to `quote_line_discount` (the line, the lever reference, the label, the amount); the **gift lines** (`gift = true`) to `quote_gift` (the gift, product, quantity, taxable flag, value and tax). Alternative: store the gift as a quote_line — rejected: `UNIQUE (quote_id, product_id)` collides with a paid line for the same product, and a gift is not a paid line.

**A discount entry carries its line.** Today the response's discount entries carry the lever and the total (e.g. one combo entry of 19000 for two covered lines), while the B1 table needs `(line, lever, amount)`. The engine therefore adds the product to `DiscountBreakdown` and the combo and scale emit **one entry per covered line** (its share); the gift entry stays on the gifted line. Alternative: derive the split from the line's total discount — impossible when two levers touch the same line.

**The `quote_gift` table resolves the pricing-gift open question.** Each gift line freezes the lever id, product, quantity, taxable flag, value and tax, so a gift of a product that is also paid has its own entry.

**The quote is append-only.** The quote and its children are written once in one transaction; a later price or rule change never alters them. The append-only guard already covers `quote` and `quote_line`; `quote_line_discount` and `quote_gift` are added to it.

**Tenant-consistent composite foreign keys.** `customer` and `quote` gain `UNIQUE (tenant_id, id)`; `quote` and `sales_order` reference the customer with `(tenant_id, customer_id)`, and the order its quote with `(tenant_id, quote_id)`. This replaces the single-column customer/quote foreign keys (the composite implies them) and makes a cross-tenant reference impossible at the database. Alternative: app-only validation — rejected: forgettable, and raw inserts bypass it.

**The endpoint.** `POST /api/v1/quotes` prices the cart and, in one transaction, writes the quote. It returns **201** with the public id, the expiry (24 hours from the clock) and the breakdown. A customer that does not belong to the cart's tenant is rejected by a pre-check (`MissingCustomerException`, a client error); the composite FK is the backstop.

## Risks / Trade-offs

- **The FK migration** validates the existing rows; the seed and tests are consistent. On large data, add the constraints `NOT VALID` and `VALIDATE` separately.
- **The gift is not a `quote_line`**: a reader merges `quote_line` and `quote_gift` for the full picture (the response already presents both as lines).
- **The quote is a snapshot**: an expired quote is rejected by the confirmation (A2).

## Open Questions

- None.
