# ADR — Flujo de pedidos B2B multi-tenant

## Contexto

Marketplace B2B multi-tenant (catálogo → carrito → cotización → confirmación → historial). Implementé pricing, cotización, confirmación y consulta. Invariantes: el total cotizado y el confirmado no divergen, y un 201 implica pedido existente, consultable y cupo consumido una vez.

## Decisiones

- **Java 21 + WebFlux + JDBC + Flyway + H2.** Elegí Java por comodidad; JDBC expone SQL y constraints y H2 permite correr todo con un comando. Descarté JPA y un Postgres real.
- **Multi-tenant por fila.** `tenant_id` en cada tabla, lecturas scoped y FKs compuestas `(tenant_id, id)`. Descarté confiar solo en la aplicación.
- **Palancas por prioridad con consumo de unidades.** Cada palanca consume lo que descuenta, sin doble descuento. Con el seed (combo 20, escala 10, obsequio 5) el orden es combo → escala → obsequio → impuesto: el combo cubre unidades concretas, la escala toma las restantes, el obsequio se evalúa sobre el bruto y el impuesto sobre la base neta. Desempate: tipo, objetivo (SKU antes que familia), id mayor. Descarté un optimizador por total y solapamientos.
- **Combo, escala y obsequio.** Combo por prioridad, mínimo como piso; escala aplica el tramo alcanzado a todas las unidades (SKU gana a familia); obsequio = línea marcada + `GIFT`, gravable sobre el bruto y persistido en `quote_gift` para no chocar con `UNIQUE (quote_id, product_id)`.
- **Dinero exacto y multi-país.** `BigDecimal`, `DECIMAL(19,4)` y `DECIMAL(7,4)`; moneda, escala y redondeo salen del ruleset (GTQ 2, COP 0, HALF_UP). Redondeo por línea; el total suma líneas redondeadas (`Money`). Descarté `double` y redondear solo el total.
- **Cotización inmutable.** Congelo ruleset, precios, tasas, líneas y total; la confirmación no re-cotiza. Descarté re-cotizar al confirmar (el bug del enunciado).
- **Cupo de crédito.** `creditFit` (`FITS`, `INSUFFICIENT_CREDIT`, `NO_ACCOUNT`) sobre el total final sin lock; la confirmación reverifica con lock y consume.
- **Confirmación idempotente y atómica.** Una transacción escribe orden, clave, cupo y outbox; respondo 201 tras el commit, con `UNIQUE (quote_id)` y `UNIQUE (customer_id, idem_key)`. Misma clave y cotización → mismo 201 (`replayed`); otra clave → 409 `QUOTE_ALREADY_CONFIRMED`; clave en otra cotización → 409 `IDEMPOTENCY_CONFLICT`; el doble tap relee al ganador. Descarté fallar el reintento y responder 200 en el replay.
- **Outbox transaccional.** `PUSH` (aviso al tendero) y `ERP` en la misma transacción; at-least-once con dedup por id de evento. Si el externo cae queda pendiente con backoff y dead-letter; la confirmación no depende de él. Relay no implementado.
- **Errores y seguridad.** Sin ids internos ni stack traces; lecturas scoped y 404 en vez de 403. Sin cache: en producción cachearía el ruleset por versión inmutable. Sin autenticación ni autorización: fuera de alcance; el tenant se confía del header y en producción vendría del token, con autorización y rate limiting.

## Qué no implementé

Cache, autenticación/autorización, relay del outbox, front y catálogo/carrito/historial completos; el porqué está en las decisiones y el alcance.

## Code review

- Aislamiento multi-tenant y límites transaccionales (sin I/O externo; 201 tras el commit).
- Idempotencia (constraints, replay, carreras) y dinero (cero `double`, escala del ruleset, redondeo por línea).
- Determinismo del pipeline y tests de invariantes (concurrencia, replay, aislamiento, cambio de precio).

## Riesgo

El pipeline determinista puede no ser el óptimo económico. Lo mitigo con prioridades configurables por ruleset, tests dorados y el origen de cada descuento; una optimización sería una estrategia explícita y versionada.

## Cómo correrlo

`.\verify-and-run.bat` (Windows) o `./verify-and-run.sh` corren los tests y, si pasan, levantan la API en `:8080`; también `gradlew.bat clean test` y `gradlew.bat bootRun`. [README](https://github.com/diegodob/marketplaceb2b/blob/main/README.md), [DER](https://github.com/diegodob/marketplaceb2b/blob/main/docs/DER.md), [CURL-EXAMPLES](https://github.com/diegodob/marketplaceb2b/blob/main/docs/CURL-EXAMPLES.md), [GoldenSetTest](https://github.com/diegodob/marketplaceb2b/blob/main/src/test/java/com/marketplaceb2b/service/GoldenSetTest.java).
