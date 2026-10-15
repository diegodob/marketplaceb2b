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
