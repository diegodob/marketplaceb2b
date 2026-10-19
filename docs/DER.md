# DER — Entidades principales

Modelo de datos del flujo de pedidos. Se muestran las entidades y columnas clave; las tablas completas están en `src/main/resources/db/migration`.

Las tablas de negocio llevan `tenant_id` y las referencias críticas usan FKs compuestas `(tenant_id, id)`, de modo que una referencia entre marcas es imposible en la base. Los importes son `DECIMAL(19,4)` y las tasas `DECIMAL(7,4)`.

## A.1 Tenancy, impuestos, productos y precios

```mermaid
erDiagram
    COUNTRY ||--o{ TENANT : "define"
    COUNTRY ||--o{ FISCAL_ID_TYPE : "define"
    TENANT ||--o{ RULESET : "publica"
    TENANT ||--o{ PRODUCT : "vende"
    TENANT ||--o{ PRODUCT_FAMILY : "agrupa"
    TENANT ||--o{ PAYMENT_METHOD : "acepta"
    TENANT ||--o{ CUSTOMER : "atiende"
    PRODUCT_FAMILY ||--o{ PRODUCT : "contiene"
    RULESET ||--o{ TAX_RATE : "fija"
    TAX_CATEGORY ||--o{ TAX_RATE : "clasifica"
    RULESET ||--o{ PRODUCT_PRICE : "precia"
    PRODUCT ||--o{ PRODUCT_PRICE : "tiene"
    TAX_CATEGORY ||--o{ PRODUCT_PRICE : "grava"

    RULESET {
        BIGINT id PK
        BIGINT tenant_id FK
        VARCHAR version
        CHAR currency
        SMALLINT currency_scale
        VARCHAR rounding_mode
    }
    PRODUCT_PRICE {
        BIGINT id PK
        BIGINT ruleset_id FK
        BIGINT product_id FK
        BIGINT tax_category_id FK
        DECIMAL unit_price
    }
    CUSTOMER {
        BIGINT id PK
        BIGINT tenant_id FK
        VARCHAR name
        VARCHAR fiscal_id
    }
```

## A.2 Palancas

```mermaid
erDiagram
    RULESET ||--o{ COMBO : "ofrece"
    COMBO ||--o{ COMBO_ITEM : "incluye"
    PRODUCT ||--o{ COMBO_ITEM : "participa"
    RULESET ||--o{ SCALE_DISCOUNT : "ofrece"
    SCALE_DISCOUNT ||--o{ SCALE_TIER : "tramos"
    PRODUCT ||--o{ SCALE_DISCOUNT : "objetivo SKU"
    PRODUCT_FAMILY ||--o{ SCALE_DISCOUNT : "objetivo familia"
    RULESET ||--o{ GIFT : "ofrece"
    GIFT }o--|| GIFT_CONDITION : "requiere"
    PRODUCT ||--o{ GIFT_CONDITION : "condición SKU"
    PRODUCT_FAMILY ||--o{ GIFT_CONDITION : "condición familia"
    PRODUCT ||--o{ GIFT : "obsequia"

    COMBO {
        BIGINT id PK
        BIGINT ruleset_id FK
        VARCHAR name
        DECIMAL combo_price
        INT priority
    }
    SCALE_DISCOUNT {
        BIGINT id PK
        BIGINT ruleset_id FK
        VARCHAR target_type
        BIGINT product_id FK
        BIGINT family_id FK
        INT priority
    }
    SCALE_TIER {
        BIGINT id PK
        BIGINT scale_discount_id FK
        INT from_qty
        DECIMAL percentage
    }
    GIFT {
        BIGINT id PK
        BIGINT ruleset_id FK
        BIGINT condition_id FK
        BIGINT gift_product_id FK
        INT gift_qty
        BOOLEAN taxable
        INT priority
    }
```

## A.3 Cotización, pedido y efectos

```mermaid
erDiagram
    CUSTOMER ||--o| CREDIT_ACCOUNT : "tiene"
    CUSTOMER ||--o{ QUOTE : "pide"
    QUOTE ||--o{ QUOTE_LINE : "detalla"
    PRODUCT ||--o{ QUOTE_LINE : "aparece"
    QUOTE_LINE ||--o{ QUOTE_LINE_DISCOUNT : "descuentos"
    COMBO ||--o{ QUOTE_LINE_DISCOUNT : "origen"
    SCALE_DISCOUNT ||--o{ QUOTE_LINE_DISCOUNT : "origen"
    GIFT ||--o{ QUOTE_LINE_DISCOUNT : "origen"
    QUOTE ||--o{ QUOTE_GIFT : "obsequios"
    GIFT ||--o{ QUOTE_GIFT : "origen"
    PRODUCT ||--o{ QUOTE_GIFT : "obsequiado"
    QUOTE ||--o| SALES_ORDER : "confirma"
    SALES_ORDER ||--o{ IDEMPOTENCY_KEY : "una clave"
    SALES_ORDER ||--o{ CREDIT_LEDGER : "consume"
    CREDIT_ACCOUNT ||--o{ CREDIT_LEDGER : "registra"
    SALES_ORDER ||--o{ OUTBOX : "emite"

    QUOTE {
        BIGINT id PK
        UUID public_id UK
        BIGINT tenant_id FK
        BIGINT customer_id FK
        BIGINT ruleset_id FK
        BIGINT payment_method_id FK
        DECIMAL total
        TIMESTAMP expires_at
    }
    QUOTE_LINE {
        BIGINT id PK
        BIGINT quote_id FK
        BIGINT product_id FK
        INT qty
        DECIMAL unit_price
        DECIMAL taxable_base
        DECIMAL tax_rate
        DECIMAL tax
    }
    QUOTE_LINE_DISCOUNT {
        BIGINT id PK
        BIGINT quote_line_id FK
        BIGINT combo_id FK
        BIGINT scale_discount_id FK
        BIGINT gift_id FK
        VARCHAR detail
        DECIMAL amount
    }
    QUOTE_GIFT {
        BIGINT id PK
        BIGINT quote_id FK
        BIGINT gift_id FK
        BIGINT product_id FK
        INT qty
        BOOLEAN taxable
        DECIMAL gift_value
        DECIMAL tax
    }
    SALES_ORDER {
        BIGINT id PK
        VARCHAR public_number UK
        BIGINT tenant_id FK
        BIGINT quote_id FK
        BIGINT customer_id FK
        TIMESTAMP created_at
    }
    IDEMPOTENCY_KEY {
        BIGINT id PK
        BIGINT customer_id FK
        VARCHAR idem_key
        BIGINT sales_order_id FK
    }
    OUTBOX {
        BIGINT id PK
        BIGINT sales_order_id FK
        VARCHAR destination
        VARCHAR event_type
        VARCHAR status
        INT attempts
        TIMESTAMP next_attempt_at
        TIMESTAMP sent_at
    }
```

Volver al [ADR](https://github.com/diegodob/marketplaceb2b/blob/main/docs/ADR.md).
