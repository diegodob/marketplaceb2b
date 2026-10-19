# Ejemplos con curl

Requiere la API levantada (`.\verify-and-run.bat`, o `gradlew.bat bootRun`) y los ids del seed de `Bebidas GT`: tenant `1`, cliente `Cliente 12` con id `1`, medio `CREDIT` con id `1`, productos `GAS-500` = `1`, `AGU-500` = `2` y `LEC-1L` = `3`.

Los importes viajan con punto decimal. Las respuestas de pedido serializan el total con la escala de la columna (4 decimales); las de pricing, con la escala de la moneda.

## B.1 Pricing — `POST /api/v1/pricing`

Carrito: GAS-500 ×6 y AGU-500 ×6 (aplica el combo `Gaseosa + Agua x6` a 20000).

```bash
curl -s -X POST http://localhost:8080/api/v1/pricing \
  -H "Content-Type: application/json" \
  -d '{"tenantId":1,"customerId":1,"paymentMethodId":1,"lines":[{"productId":1,"qty":6},{"productId":2,"qty":6}]}'
```

```json
{
  "tenantId": 1,
  "rulesetId": 1,
  "currency": "GTQ",
  "lines": [
    { "productId": 1, "qty": 6, "unitPrice": 4000.0000, "gross": 24000.00, "discount": 11692.31,
      "taxableBase": 12307.69, "taxRate": 12.0000, "tax": 1476.92, "gift": false },
    { "productId": 2, "qty": 6, "unitPrice": 2500.0000, "gross": 15000.00, "discount": 7307.69,
      "taxableBase": 7692.31, "taxRate": 12.0000, "tax": 923.08, "gift": false }
  ],
  "discounts": [
    { "productId": 1, "origin": "COMBO", "sourceId": 1, "detail": "Gaseosa + Agua x6", "amount": 11692.31 },
    { "productId": 2, "origin": "COMBO", "sourceId": 1, "detail": "Gaseosa + Agua x6", "amount": 7307.69 }
  ],
  "grossSubtotal": 39000.00,
  "totalDiscounts": 19000.00,
  "totalTaxableBase": 20000.00,
  "totalTax": 2400.00,
  "total": 22400.00,
  "creditFit": { "fits": true, "available": 300000.00, "total": 22400.00, "reason": "FITS" }
}
```

## B.2 Cotización — `POST /api/v1/quotes`

Mismo carrito. Responde 201 con la referencia pública y el vencimiento a 24 horas.

```bash
curl -s -X POST http://localhost:8080/api/v1/quotes \
  -H "Content-Type: application/json" \
  -d '{"tenantId":1,"customerId":1,"paymentMethodId":1,"lines":[{"productId":1,"qty":6},{"productId":2,"qty":6}]}'
```

```json
{
  "publicId": "8f14e45f-ceea-4b4a-9f0e-2d6c1f7a1b23",
  "expiresAt": "2026-10-20T10:00:00Z",
  "breakdown": { "...": "mismo shape que POST /pricing" }
}
```

## B.3 Confirmación — `POST /api/v1/orders`

```bash
curl -i -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 1" \
  -H "Idempotency-Key: 7d3f9a1e-2c4b-4e6d-9a8f-1b2c3d4e5f60" \
  -d '{ "quotePublicId": "8f14e45f-ceea-4b4a-9f0e-2d6c1f7a1b23" }'
```

```json
{ "number": "5c2f1d6a-9b8e-4a7c-8f1d-2e3b4c5d6e7f", "total": 22400.0000, "replayed": false }
```

Reintento con la misma clave y cotización: mismo 201 y mismo `number`, con `"replayed": true`; no crea otra orden ni vuelve a consumir cupo.

## B.4 Consulta — `GET /api/v1/orders/{number}`

```bash
curl -s http://localhost:8080/api/v1/orders/5c2f1d6a-9b8e-4a7c-8f1d-2e3b4c5d6e7f \
  -H "X-Tenant-Id: 1"
```

```json
{
  "number": "5c2f1d6a-9b8e-4a7c-8f1d-2e3b4c5d6e7f",
  "quotePublicId": "8f14e45f-ceea-4b4a-9f0e-2d6c1f7a1b23",
  "total": 22400.0000,
  "createdAt": "2026-10-19T10:00:00Z",
  "lines": [
    { "id": 12, "quoteId": 7, "productId": 1, "qty": 6, "unitPrice": 4000.0000,
      "taxableBase": 12307.6900, "taxRate": 12.0000, "tax": 1476.9200 },
    { "id": 13, "quoteId": 7, "productId": 2, "qty": 6, "unitPrice": 2500.0000,
      "taxableBase": 7692.3100, "taxRate": 12.0000, "tax": 923.0800 }
  ]
}
```

Otro tenant con el mismo número recibe 404.

## B.5 Errores

| Caso | Respuesta |
|---|---|
| Clave usada en otra cotización | `409 {"code":"IDEMPOTENCY_CONFLICT","message":"La clave de idempotencia ya se usó para otra cotización.","details":{}}` |
| Cotización ya confirmada con otra clave | `409 {"code":"QUOTE_ALREADY_CONFIRMED","message":"La cotización ya tiene un pedido confirmado.","details":{}}` |
| Cotización inexistente o de otro tenant | `404 {"code":"QUOTE_NOT_FOUND","message":"La cotización no existe o no pertenece a esta tienda.","details":{}}` |
| Cupo insuficiente | `422 {"code":"INSUFFICIENT_CREDIT","message":"El cupo disponible no alcanza para confirmar el pedido.","details":{}}` |
| Cotización vencida | `422 {"code":"QUOTE_EXPIRED","message":"La cotización venció. Volvé a cotizar.","details":{}}` |
| Falta header o body inválido | `400 {"code":"INVALID_REQUEST","message":"La solicitud no es válida.","details":{}}` |

Volver al [ADR](https://github.com/diegodob/marketplaceb2b/blob/main/docs/ADR.md) o al [DER](https://github.com/diegodob/marketplaceb2b/blob/main/docs/DER.md).
