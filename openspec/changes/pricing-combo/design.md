# Design

## Context

`pricing-core` computes gross, tax and total with no levers. The lever tables, their repositories and a `priority` per lever (`lever-priority`) exist. This change adds the engine's lever **pipeline** and the **combo** strategy. See proposal.md and `specs/order-pricing/spec.md`.

## Goals / Non-Goals

**Goals:**
- Apply the applicable levers in a fixed, explicit order driven by each lever's `priority` (the distributor decides, not an inference), exclusive over consumed units.
- The combo step, predictable and documented (never minimizing the total).

**Non-Goals:**
- The scale and gift strategies' own rules (their changes); quote creation; the credit-fits check.

## Decisions

**Pipeline of strategies, priority-driven.** A `LeverStrategy` per lever type computes that lever's effect on the pricing context. `PricingPipeline` collects the ruleset's **applicable** levers (each type decides what "applicable" means), orders them by **descending `priority`** (ties by `id`), and applies each through its type's strategy. Adding a lever type is adding a strategy. Alternative: type-ordered procedural steps — rejected: rigid and not data-driven.

**Exclusivity via consumed units.** Each applied lever **consumes** the units it discounts (per product). A lower-priority lever only sees the remaining units, so "the combo excludes the scale" is automatic and no lever needs to know the others.

**PricingContext.** Carries the cart, the ruleset (currency scale and rounding mode), the loaded prices/rates and the mutable accumulators `discounts` (list of `DiscountBreakdown`), `gifts` (list of `GiftBreakdown`) and, per product, the consumed units. A line keeps its **list gross** (`qty × unitPrice`); each applied lever adds to that line's **discount**; the line's **taxable base** is `gross − discount`. The discount entry is a **report** — `origin` (the lever type), the lever's `sourceId` and a label, plus the amount — traceable to the lever that produced it. `OrderPricingService.price(cart)` builds the context, runs the pipeline, then computes the per-line taxable base and the tax on top.

**Combo strategy.** A combo is **applicable** when every item's **still-available** units reach its minimum. When applied (in priority order), it covers exactly `minQty` units per item: each covered line keeps its **list gross** and gets a **discount** equal to its share of `(covered list value − combo price)`, prorated across the items by their share of the covered list value (the last item takes the rounding remainder). The **combo discount entry** carries — with its origin (the lever type and its `sourceId`) — the total amount. Units above the minimum keep the regular price; the covered units are consumed. If several combos apply, the higher `priority` runs first and a later combo only sees the units the earlier ones left (so it may no longer be applicable).

## Ejemplo: combo de dos productos con impuestos distintos

Datos del seed: `GAS-500` 4000 (GENERAL 21%), `YER-1KG` 6000 (REDUCED 10.5%). Combo `{GAS-500 mín 6, YER-1KG mín 3}` a **35000**. Carrito: GAS-500 ×6, YER-1KG ×3 (ninguna otra palanca aplica).

Valor de lista cubierto = 6×4000 + 3×6000 = 42000; descuento = 42000 − 35000 = 7000, repartido por valor de lista (4/7 y 3/7).

| Línea | qty | unit | gross (lista) | discount | base = gross − disc | tasa | tax |
|---|---|---|---|---|---|---|---|
| GAS-500 | 6 | 4000 | 24000 | 4000 | 20000 | 21% | 4200.00 |
| YER-1KG | 3 | 6000 | 18000 | 3000 | 15000 | 10.5% | 1575.00 |
| **Totales** | | | **42000** | **7000** | **35000** | | **5775.00** |

Entrada de descuento: `origin=COMBO, sourceId=<id del combo>, amount=7000.00`. `Σ base` = 35000 (= precio del combo); total a pagar = 35000 + 5775 = **40775.00**.

## Risks / Trade-offs

- **Prorating the combo discount across its items** → the covered lines keep their list gross and each takes a discount share (by list value); then `Σ base` equals the combo price and every line keeps a base for the tax step. Documented.
- **The engine never minimizes the total** → the buyer may not get the theoretical best price, but the result is predictable and auditable; the distributor controls the order via `priority`.
- **Pipeline adds indirection** → small; each strategy is independently testable, and the engine keeps its current tests.

## Follow-up: persistir el descuento aplicado (B1)

Decisión: el descuento aplicado se **congela** al emitir la cotización, no se recalcula contra la configuración (que es mutable). Por eso el desglose que produce el motor (cada entrada con su tipo de palanca y su id) se **persiste por línea**, con **FK reales tipadas** (sin `source_id` polimórfico ni `source_type` no verificable):

```sql
quote_line_discount (
    id, quote_line_id FK,
    scale_discount_id FK NULL, combo_id FK NULL, gift_id FK NULL,
    detail, amount,
    CHECK (exactly one of the three lever ids is NOT NULL)   -- la que aplicó
)
```
(opcional: `quote.subtotal`, `quote.discount_total`, `quote.tax_total`).

En la respuesta, `origin` identifica el tipo de palanca y `sourceId` el id de la fila; en la BD eso corresponde a **cuál** de las tres columnas FK quedó seteada (`detail` guarda el nombre del combo/escala tal como se emitió).

Motivo: `quote_line` guarda `unit_price` (lista) y `taxable_base` (neta), así que el importe es derivable (`qty × unit_price − taxable_base`), pero el **origen** y el **reparto por palanca** se pierden; además, con la config editable (`combo.combo_price`, `scale_tier.percentage`, `priority`) un recálculo posterior diverge de lo guardado. Persistir el descuento con su origen mantiene la cotización auditable e inmutable.

Alcance: se implementa en `quote-creation` (creación de la cotización), no en el motor de pricing.

## Open Questions

- None.
