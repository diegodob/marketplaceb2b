# Proposal

## Why

The pricing pipeline and the combo lever exist, but the scale discount — one of the take-home's commercial levers — is not applied. A cart that reaches a scale tier is still charged the list price.

## What Changes

- Add the **scale strategy**: a scale targets a SKU or a family; it applies when the target's available units reach its first tier; the **reached tier** (the greatest `from_qty` not above those units) percentage applies to **all** of the target's available units; a family scale sums its products' units; the covered units are consumed and the saving is reported as a scale entry with its origin.
- Add a **tenant-scoped product lookup** so a family scale can resolve which cart products belong to the family (the access layer must scope by tenant and prove it with a multi-tenant test, as `data-model` requires).
- Keep the pipeline's rule: scales are applied by descending priority and never by choosing the larger discount; the higher-priority scale consumes the units, so a lower one sees the remainder.
- **Out of scope:** the gift strategy (its own slice), quote creation and the credit-fits check.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `order-pricing`: adds the scale step (reached-tier percentage on all the target's units, targets, consumption and reporting).

## Impact

- New `ScaleDiscountStrategy` in `service.pricing`; `PricingContext` carries the scales and the cart products' families; `OrderPricingService` loads the scales and the products (tenant-scoped).
- New `ProductRepository` (tenant-scoped lookup) and a multi-tenant test for it.
- Tests for the scale step; the README's scale note is corrected (priority, not "greater discount").
