# Design

## Context

El seed de Bebidas GT trae los datos necesarios: `GAS-500` 4000 al 12 %, `AGU-500` 2500 al 12 %, `LEC-1L` 6000 al 0 %; escala de SKU `GAS-500` 12→3 % y 24→7 %; escala de familia `Gaseosas` 10→5 % y 50→10 %; combo `Gaseosa + Agua x6` a 20000; obsequios sobre `AGU-500`; cupo de crédito 300000.

## Goals

- Una sola suite legible que cubra los seis casos mínimos del enunciado.
- Ejercitar el **contrato HTTP** (validación, serialización, headers, errores), no solo el servicio.
- Expectativas fijas y explícitas (importes, cantidades, orígenes y totales), no snapshots.
- Fallar si cambia cualquiera de esas conductas.

## Non-Goals

- Reemplazar las suites existentes (escala, combo, obsequio, crédito, cotización→orden).
- Probar concurrencia, idempotencia o aislamiento multi-tenant (ya cubiertos aparte).
- Cambiar código de producción.

## Decisions

- **Nivel controller.** `@SpringBootTest(RANDOM_PORT)` + `WebTestClient` contra `/api/v1/pricing`, `/api/v1/quotes`, `/api/v1/orders` y `/api/v1/orders/{number}`. Así se valida el contrato completo que consume el cliente.
- **Fixtures del seed.** Los tests usan los productos, precios, escalas y combo del seed; solo el caso 3 inserta su obsequio y el caso 1 una escala adicional para `LEC-1L`, porque el seed solo trae escalas de `GAS-500`. Los obsequios del seed se apartan en `@BeforeEach` (se guardan y se borran) y se restauran en `@AfterEach`, para que el resultado sea predecible y el contexto quede como estaba.
- **Los tests commitean.** Al pasar por HTTP no hay rollback del test: cada caso registra las cotizaciones que crea y en `@AfterEach` restaura el cupo consumido y borra el grafo (outbox, ledger, idempotency key, orden, descuentos, obsequios, líneas y cotización), más los fixtures que insertó (escalas, obsequios y condiciones).
- **Un pedido, dos tramos (caso 1).** Se inserta una escala de `LEC-1L` 10→5 % y se cotiza `GAS-500` 24 (segundo tramo, 7 %) + `LEC-1L` 10 (primer tramo, 5 %): el mismo pedido ejercita dos tramos.
- **Combo sin descuento suelto (caso 2).** Se afirma que hay exactamente dos entradas, ambas `COMBO`, cada una con su importe prorrateado, y que su suma es el descuento total; no existe una entrada única por 19000.
- **Obsequio con cantidad (caso 3).** Se inserta un obsequio de `AGU-500` ×2 por 12 unidades de `GAS-500` y se afirma la línea de regalo con `qty = 2`, valor 5000 y descuento `GIFT` 5000.
- **Impuesto del combo (caso 4).** Se afirma el impuesto por línea (1476.92 + 923.08) y que `total = base gravable + impuesto`.
- **Crédito excedido (caso 5).** Se cotiza `GAS-500` 80 (total 333312 > 300000) por HTTP, se afirma `creditFit` con `INSUFFICIENT_CREDIT` y que `POST /orders` responde **422** `INSUFFICIENT_CREDIT`, sin crear orden ni consumo.
- **Cotización y confirmación (caso 6).** `POST /quotes` y `POST /orders`; se afirma que el total del breakdown, el `total` persistido en `quote`, el del pedido, el del `GET /orders/{number}` y el consumo del ledger son el mismo importe.
- **Aislamiento.** La suite no es transaccional (los endpoints commitean); la limpieza explícita por test deja el contexto en el estado del seed.

## Risks / Trade-offs

- Los valores esperados dependen del seed: si el seed cambia, la suite falla a propósito y el cambio debe ser deliberado.
- La limpieza es explícita: si un test falla antes de registrar una cotización, podría dejar filas. Se registra la cotización apenas se crea para minimizar la ventana.
- El caso 1 inserta una escala extra; está documentado en el propio test para que no se confunda con datos del seed.

## Migration Plan

- Ninguna.

## Open Questions

- Ninguna.
