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
