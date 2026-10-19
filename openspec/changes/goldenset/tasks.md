## 1. Golden set

- [x] 1.1 Caso 1: escala con dos tramos en un mismo pedido, con importes fijos.
- [x] 1.2 Caso 2: combo sin descuento suelto (dos entradas `COMBO`, suma = descuento total).
- [x] 1.3 Caso 3: obsequio visible en el desglose con su cantidad.
- [x] 1.4 Caso 4: combo con impuestos, verificando que entran en el total.
- [x] 1.5 Caso 5: crédito que excede el cupo (preview y rechazo de la confirmación).
- [x] 1.6 Caso 6: cotización y confirmación con el mismo total.

## 2. Verificación

- [x] 2.1 `gradlew.bat clean test` en verde.
- [x] 2.2 `openspec validate goldenset --strict`.
