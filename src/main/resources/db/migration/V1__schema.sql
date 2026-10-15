CREATE TABLE country (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(2) NOT NULL,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT uq_country_code UNIQUE (code)
);

CREATE TABLE fiscal_id_type (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    country_id BIGINT NOT NULL,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT uq_fiscal_id_type_code UNIQUE (country_id, code),
    CONSTRAINT fk_fiscal_id_type_country FOREIGN KEY (country_id) REFERENCES country (id)
);
CREATE INDEX ix_fiscal_id_type_country_id ON fiscal_id_type (country_id);

CREATE TABLE tenant (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    country_id BIGINT NOT NULL,
    CONSTRAINT uq_tenant_name UNIQUE (name),
    CONSTRAINT fk_tenant_country FOREIGN KEY (country_id) REFERENCES country (id)
);
CREATE INDEX ix_tenant_country_id ON tenant (country_id);

CREATE TABLE tax_category (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT uq_tax_category_code UNIQUE (code)
);

CREATE TABLE ruleset (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    version VARCHAR(50) NOT NULL,
    currency CHAR(3) NOT NULL,
    currency_scale SMALLINT NOT NULL,
    rounding_mode VARCHAR(10) NOT NULL,
    effective_from TIMESTAMP NOT NULL,
    CONSTRAINT uq_ruleset_version UNIQUE (tenant_id, version),
    CONSTRAINT uq_ruleset_effective UNIQUE (tenant_id, effective_from),
    CONSTRAINT fk_ruleset_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);
CREATE INDEX ix_ruleset_tenant_id ON ruleset (tenant_id);

CREATE TABLE tax_rate (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ruleset_id BIGINT NOT NULL,
    tax_category_id BIGINT NOT NULL,
    rate DECIMAL(7,4) NOT NULL,
    CONSTRAINT uq_tax_rate_category UNIQUE (ruleset_id, tax_category_id),
    CONSTRAINT ck_tax_rate_range CHECK (rate >= 0 AND rate <= 100),
    CONSTRAINT fk_tax_rate_ruleset FOREIGN KEY (ruleset_id) REFERENCES ruleset (id),
    CONSTRAINT fk_tax_rate_category FOREIGN KEY (tax_category_id) REFERENCES tax_category (id)
);
CREATE INDEX ix_tax_rate_ruleset_id ON tax_rate (ruleset_id);
CREATE INDEX ix_tax_rate_tax_category_id ON tax_rate (tax_category_id);

CREATE TABLE product (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    sku VARCHAR(60) NOT NULL,
    name VARCHAR(200) NOT NULL,
    family VARCHAR(80),
    CONSTRAINT uq_product_sku UNIQUE (tenant_id, sku),
    CONSTRAINT fk_product_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);

CREATE TABLE product_price (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ruleset_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    tax_category_id BIGINT NOT NULL,
    unit_price DECIMAL(19,4) NOT NULL,
    CONSTRAINT uq_product_price_version UNIQUE (ruleset_id, product_id),
    CONSTRAINT ck_product_price_nonnegative CHECK (unit_price >= 0),
    CONSTRAINT fk_product_price_ruleset FOREIGN KEY (ruleset_id) REFERENCES ruleset (id),
    CONSTRAINT fk_product_price_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT fk_product_price_tax_category FOREIGN KEY (tax_category_id) REFERENCES tax_category (id)
);
CREATE INDEX ix_product_price_ruleset_id ON product_price (ruleset_id);
CREATE INDEX ix_product_price_product_id ON product_price (product_id);
CREATE INDEX ix_product_price_tax_category_id ON product_price (tax_category_id);

CREATE TABLE payment_method (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    uses_credit BOOLEAN NOT NULL,
    CONSTRAINT uq_payment_method_code UNIQUE (tenant_id, code),
    CONSTRAINT fk_payment_method_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);

CREATE TABLE customer (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    fiscal_id VARCHAR(30) NOT NULL,
    fiscal_id_type_id BIGINT NOT NULL,
    CONSTRAINT fk_customer_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
    CONSTRAINT fk_customer_fiscal_id_type FOREIGN KEY (fiscal_id_type_id) REFERENCES fiscal_id_type (id)
);
CREATE INDEX ix_customer_tenant_id ON customer (tenant_id);
CREATE INDEX ix_customer_fiscal_id_type_id ON customer (fiscal_id_type_id);

CREATE TABLE credit_account (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    currency CHAR(3) NOT NULL,
    credit_limit DECIMAL(19,4) NOT NULL,
    available DECIMAL(19,4) NOT NULL,
    CONSTRAINT uq_credit_account_customer UNIQUE (customer_id),
    CONSTRAINT ck_credit_account_balance CHECK (available >= 0 AND available <= credit_limit),
    CONSTRAINT fk_credit_account_customer FOREIGN KEY (customer_id) REFERENCES customer (id)
);

CREATE TABLE quote (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id UUID NOT NULL,
    tenant_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    ruleset_id BIGINT NOT NULL,
    payment_method_id BIGINT NOT NULL,
    total DECIMAL(19,4) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_quote_public_id UNIQUE (public_id),
    CONSTRAINT fk_quote_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
    CONSTRAINT fk_quote_customer FOREIGN KEY (customer_id) REFERENCES customer (id),
    CONSTRAINT fk_quote_ruleset FOREIGN KEY (ruleset_id) REFERENCES ruleset (id),
    CONSTRAINT fk_quote_payment_method FOREIGN KEY (payment_method_id) REFERENCES payment_method (id)
);
CREATE INDEX ix_quote_tenant_id ON quote (tenant_id);
CREATE INDEX ix_quote_customer_id ON quote (customer_id);
CREATE INDEX ix_quote_ruleset_id ON quote (ruleset_id);
CREATE INDEX ix_quote_payment_method_id ON quote (payment_method_id);

CREATE TABLE quote_line (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    quote_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    qty INTEGER NOT NULL,
    unit_price DECIMAL(19,4) NOT NULL,
    taxable_base DECIMAL(19,4) NOT NULL,
    tax_rate DECIMAL(7,4) NOT NULL,
    tax DECIMAL(19,4) NOT NULL,
    CONSTRAINT uq_quote_line_product UNIQUE (quote_id, product_id),
    CONSTRAINT ck_quote_line_qty CHECK (qty > 0),
    CONSTRAINT fk_quote_line_quote FOREIGN KEY (quote_id) REFERENCES quote (id),
    CONSTRAINT fk_quote_line_product FOREIGN KEY (product_id) REFERENCES product (id)
);
CREATE INDEX ix_quote_line_product_id ON quote_line (product_id);

CREATE TABLE sales_order (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_number VARCHAR(40) NOT NULL,
    tenant_id BIGINT NOT NULL,
    quote_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP,
    CONSTRAINT uq_sales_order_public_number UNIQUE (public_number),
    CONSTRAINT uq_sales_order_quote UNIQUE (quote_id),
    CONSTRAINT fk_sales_order_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
    CONSTRAINT fk_sales_order_quote FOREIGN KEY (quote_id) REFERENCES quote (id),
    CONSTRAINT fk_sales_order_customer FOREIGN KEY (customer_id) REFERENCES customer (id)
);
CREATE INDEX ix_sales_order_tenant_id ON sales_order (tenant_id);
CREATE INDEX ix_sales_order_customer_id ON sales_order (customer_id);

CREATE TABLE idempotency_key (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    idem_key VARCHAR(100) NOT NULL,
    sales_order_id BIGINT NOT NULL,
    CONSTRAINT uq_idempotency_key UNIQUE (customer_id, idem_key),
    CONSTRAINT fk_idempotency_customer FOREIGN KEY (customer_id) REFERENCES customer (id),
    CONSTRAINT fk_idempotency_sales_order FOREIGN KEY (sales_order_id) REFERENCES sales_order (id)
);
CREATE INDEX ix_idempotency_sales_order_id ON idempotency_key (sales_order_id);

CREATE TABLE credit_ledger (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    credit_account_id BIGINT NOT NULL,
    sales_order_id BIGINT NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    entry_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_credit_ledger_order_type UNIQUE (sales_order_id, entry_type),
    CONSTRAINT ck_credit_ledger_entry_type CHECK (entry_type IN ('CONSUMPTION', 'REVERSAL')),
    CONSTRAINT fk_credit_ledger_account FOREIGN KEY (credit_account_id) REFERENCES credit_account (id),
    CONSTRAINT fk_credit_ledger_sales_order FOREIGN KEY (sales_order_id) REFERENCES sales_order (id)
);
CREATE INDEX ix_credit_ledger_account_id ON credit_ledger (credit_account_id);

CREATE TABLE outbox (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sales_order_id BIGINT NOT NULL,
    destination VARCHAR(10) NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(10) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP,
    last_error TEXT,
    created_at TIMESTAMP NOT NULL,
    sent_at TIMESTAMP,
    CONSTRAINT uq_outbox_event UNIQUE (sales_order_id, destination, event_type),
    CONSTRAINT ck_outbox_destination CHECK (destination IN ('PUSH', 'ERP')),
    CONSTRAINT ck_outbox_event_type CHECK (event_type IN ('ORDER_CONFIRMED', 'ORDER_CANCELLED')),
    CONSTRAINT ck_outbox_status CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    CONSTRAINT fk_outbox_sales_order FOREIGN KEY (sales_order_id) REFERENCES sales_order (id)
);
CREATE INDEX ix_outbox_relay ON outbox (status, next_attempt_at);
