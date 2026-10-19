# Proposal

## Why

El enunciado exige un conjunto mínimo de **golden tests** con seis casos. Hoy las conductas están cubiertas por suites separadas (escala, combo, obsequio, crédito y cotización→orden), pero no hay una suite única, con expectativas fijas, que un evaluador pueda leer de punta a punta y que falle si cambia cualquiera de esas conductas.

## What Changes

- Agregar `GoldenSetTest`, con seis tests a nivel de controller (endpoints HTTP) y resultados esperados fijos sobre el seed de Bebidas GT:
  1. descuento de escala en dos tramos dentro de un mismo pedido;
  2. combo sin que los productos que lo activan aparezcan como un descuento suelto;
  3. obsequio visible en el desglose con su cantidad;
  4. combo con impuestos, verificando que el impuesto entra en el total;
  5. pedido a crédito que excede el cupo (preview y rechazo en la confirmación);
  6. cotización y confirmación con el mismo total.
- No hay cambios en código de producción, esquema ni endpoints.

## Capabilities

### New Capabilities

- (ninguna: es un cambio de verificación)

### Modified Capabilities

- (ninguna)

## Impact

- Nuevo: `src/test/java/com/marketplaceb2b/controller/GoldenSetTest.java`.
- No cambia comportamiento ni datos; solo agrega cobertura de aceptación.
