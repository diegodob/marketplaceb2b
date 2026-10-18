# Tasks

## 1. Product lookup

- [ ] 1.1 Add a tenant-scoped `ProductRepository.findByIds(tenantId, productIds)` returning the cart's products (with `family_id`) and a multi-tenant test proving another tenant's products are not returned

## 2. Scale strategy

- [ ] 2.1 Load the cart products and the ruleset's scales in `OrderPricingService.price`, carry the product -> family mapping and the scales in `PricingContext`, and verify the project compiles
- [ ] 2.2 Implement `ScaleDiscountStrategy`: applicable when the target's available units reach the first tier; the reached tier's percentage applies to all the target's available units (a SKU scale covers one product, a FAMILY scale sums its lines); add the per-line discounts, consume the covered units and record a SCALE entry with the lever's type and id
- [ ] 2.3 Extend `ScheduledLever` with the ordering keys (lever type and target kind) and apply the deterministic tie-break in `PricingPipeline`: priority desc; on a tie, combos before scales before gifts, a SKU scale before a FAMILY scale, then the greater `id` first

## 3. Tests

- [ ] 3.1 Happy path: a SKU scale at the first tier and at a higher tier (all units at the reached percentage), a FAMILY scale summing its products, and two scales at two tiers applying in one order (the take-home's "discount in two tiers")
- [ ] 3.2 Edge cases: below the first tier (no discount); two scales on the same target (priority consumes the units, the second does not apply); a scale combined with the combo (the combo consumes its units first and the scale sees the rest); different tax rates; the CO currency scale; another tenant's scale does not apply
- [ ] 3.3 Run `gradlew.bat clean test` and confirm the full suite passes with exit code `0`

## 4. Documentation

- [ ] 4.1 Correct the README's scale note (priority over "the greater discount"; the reached tier applies its percentage to all the target's units) and document the tie-break (combo → scale → gift; SKU before FAMILY; newest id first)

## Workflow follow-up

- After the maintainer merges the PR, archive the change (`openspec archive pricing-scale`).
