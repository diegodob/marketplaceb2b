# Design

## Context

El seed de Bebidas GT trae los datos necesarios: `GAS-500` 4000 al 12 %, `AGU-500` 2500 al 12 %, `LEC-1L` 6000 al 0 %; escala de SKU `GAS-500` 12→3 % y 24→7 %; escala de familia `Gaseosas` 10→5 % y 50→10 %; combo `Gaseosa + Agua x6` a 20000; obsequios sobre `AGU-500`; cupo de crédito 300000.

## Goals

- Una sola suite legible que cubra los seis casos mínimos del enunciado.
- Expectativas fijas y explícitas (importes, cantidades, orígenes y totales), no snapshots.
- Fallar si cambia cualquiera de esas conductas.

## Non-Goals

- Reemplazar las suites existentes (escala, combo, obsequio, crédito, cotización→orden).
- Probar concurrencia, idempotencia o aislamiento multi-tenant (ya cubiertos aparte).
- Cambiar código de producción.

## Decisions

- **Fixtures del seed.** Los tests usan los productos, precios, escalas y combo del seed; solo el caso 3 inserta su obsequio y el caso 1 una escala adicional para `LEC-1L`, porque el seed no trae una segunda escala. Los obsequios del seed se borran en `@BeforeEach` para que cada caso tenga un resultado predecible.
- **Un pedido, dos tramos (caso 1).** Se inserta una escala de `LEC-1L` 10→5 % y se cotiza `GAS-500` 24 (segundo tramo, 7 %) + `LEC-1L` 10 (primer tramo, 5 %): el mismo pedido ejercita dos tramos.
- **Combo sin descuento suelto (caso 2).** Se afirma que hay exactamente dos entradas, ambas `COMBO`, cada una con su importe prorrateado, y que su suma es el descuento total; no existe una entrada única por 19000.
- **Obsequio con cantidad (caso 3).** Se inserta un obsequio de `AGU-500` ×2 por 12 unidades de `GAS-500` y se afirma la línea de regalo con `qty = 2`, valor 5000 y descuento `GIFT` 5000.
- **Impuesto del combo (caso 4).** Se afirma el impuesto por línea (1476.92 + 923.08) y que `total = base gravable + impuesto`.
- **Crédito excedido (caso 5).** Se cotiza `GAS-500` 80 (total 333312 > 300000), se afirma `creditFit` con `INSUFFICIENT_CREDIT` y, además, que la confirmación lanza `InsufficientCreditException` sin crear la orden.
- **Cotización y confirmación (caso 6).** Se crea la cotización con `QuoteWriter` y se confirma con `OrderConfirmationService`; se afirma que el total del breakdown, el total persistido en `quote` y el consumo del ledger son el mismo importe.
- **Aislamiento.** La suite extiende `AbstractIntegrationTest` (`@Transactional`), así cada test revierte sus inserciones.

## Risks / Trade-offs

- Los valores esperados dependen del seed: si el seed cambia, la suite falla a propósito y el cambio debe ser deliberado.
- El caso 1 inserta una escala extra; está documentado en el propio test para que no se confunda con datos del seed.

## Migration Plan

- Ninguna.

## Open Questions

- Ninguna.
