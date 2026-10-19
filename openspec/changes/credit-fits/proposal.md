# Proposal

## Why

A1 requires the pricing to say whether a credit order fits the customer's available credit, but the engine ignores the cart's customer and payment method: it prices the cart and nothing more.

## What Changes

- Add a **credit fit** to the breakdown: when the cart's payment method uses credit, the response carries `creditFit { fits, available, total, reason }`; otherwise it is null.
- `reason` is `FITS`, `EXCEEDS_LIMIT` or `NO_ACCOUNT` (the payment method uses credit but the customer has no credit account — no available credit).
- The pricing resolves the payment method **scoped by the cart's tenant** and reads the customer's credit account **without locking** (it is a preview; the confirmation stays the one that locks and consumes).
- A payment method id that does not resolve in the tenant is an explicit client error.
- The confirmation's own credit check is unchanged (A2).

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `order-pricing`: adds the credit fit and the tenant-scoped payment method resolution.
- `pricing-api`: the response carries the credit fit, and a payment method that cannot be resolved is a client error.

## Impact

- New `CreditFit` (and a reason) in the DTOs; `PriceBreakdown` gains it; the engine resolves the method and the account; `PaymentMethodRepository` gets a tenant-scoped find and `CreditAccountRepository` a non-locking read; the order confirmation and the tests use the scoped find.
- Tests for the fit (within, above, no account, non-credit, no method, unknown, another tenant) and the tenant-scoped lookup.
