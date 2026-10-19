# Design

## Context

See proposal.md — Why. The engine prices a cart and ignores `Cart.customerId` and `Cart.paymentMethodId`. `PaymentMethodRepository.find(id)` is **not tenant-scoped**, so a cross-tenant id resolves. `CreditAccountRepository` only exposes `lockByCustomer` (`FOR UPDATE`), which a read-only preview must not use. `PriceBreakdown` has no credit field.

## Goals / Non-Goals

**Goals:**
- Report the credit fit in the breakdown, with a reason.
- Resolve the payment method scoped by the cart's tenant and read the account without locking.

**Non-Goals:**
- The confirmation's own check and consume (A2); reserving credit (the fit is a snapshot).

## Decisions

**A nullable `creditFit`.** `{ fits, available, total, reason }`, null when the cart has no payment method or its method does not use credit. Alternative: always present with an `applicable` flag — rejected: null is unambiguous.

**A reason.** `FITS`, `INSUFFICIENT_CREDIT` or `NO_ACCOUNT`, so the client can tell "over the available credit" from "no account".

**No account is an indicator, not an error.** The preview reports `NO_ACCOUNT` and `fits: false`; the confirmation is the step that refuses the order (A2). Alternative: throw — rejected: the take-home says the calculation must *indicate*, and pricing is read-only.

**Tenant-scoped payment method.** `find(tenantId, id)` replaces the unscoped find; the order confirmation passes the quote's tenant. A cross-tenant id must not resolve (the `data-model` isolation). Alternative: keep the unscoped find — rejected: it leaks another tenant's method.

**Non-locking account read.** `CreditAccountRepository.findByCustomer` (no `FOR UPDATE`); the confirmation keeps `lockByCustomer` plus `consume`.

**The fit is computed on the final total** (after the levers and the tax) and compares it with the account's `available`. The account without an account resolves to `available: 0` and `NO_ACCOUNT`.

**A missing payment method is an error.** A `paymentMethodId` that does not resolve in the cart's tenant raises `MissingPaymentMethodException`; the API maps it to a **422** (`PAYMENT_METHOD_NOT_FOUND`).

**Same-currency assumption.** The fit compares the total (ruleset currency) with `available` (account currency); the schema does not enforce that they match, so a mismatch is a data issue accepted for this slice.

## Example (the seeded GT customer, 300000 available)

| Cart | Total | `creditFit` |
|---|---|---|
| LEC ×60 | 360000 | `{ fits: false, available: 300000, total: 360000, reason: INSUFFICIENT_CREDIT }` |
| LEC ×10 | 60000 | `{ fits: true, available: 300000, total: 60000, reason: FITS }` |
| credit method, no account | any | `{ fits: false, available: 0, total: …, reason: NO_ACCOUNT }` |
| cash method, or no method | any | `null` |

## Risks / Trade-offs

- **The fit is a snapshot** — the available credit can change before the confirmation. The confirmation re-checks under the lock.
- **`NO_ACCOUNT` in the preview** means the client must ask the distributor to open the credit account.
- **The currency assumption** (above) is not enforced by the schema.

## Open Questions

- None.
