ALTER TABLE customer ADD CONSTRAINT uq_customer_tenant_id UNIQUE (tenant_id, id);
ALTER TABLE quote ADD CONSTRAINT uq_quote_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE quote DROP CONSTRAINT fk_quote_customer;
ALTER TABLE quote ADD CONSTRAINT fk_quote_customer_tenant
    FOREIGN KEY (tenant_id, customer_id) REFERENCES customer (tenant_id, id);

ALTER TABLE sales_order DROP CONSTRAINT fk_sales_order_quote;
ALTER TABLE sales_order DROP CONSTRAINT fk_sales_order_customer;
ALTER TABLE sales_order ADD CONSTRAINT fk_sales_order_quote_tenant
    FOREIGN KEY (tenant_id, quote_id) REFERENCES quote (tenant_id, id);
ALTER TABLE sales_order ADD CONSTRAINT fk_sales_order_customer_tenant
    FOREIGN KEY (tenant_id, customer_id) REFERENCES customer (tenant_id, id);

CREATE INDEX ix_quote_tenant_customer ON quote (tenant_id, customer_id);
CREATE INDEX ix_sales_order_tenant_customer ON sales_order (tenant_id, customer_id);
CREATE INDEX ix_sales_order_tenant_quote ON sales_order (tenant_id, quote_id);

CREATE TABLE quote_line_discount (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    quote_line_id BIGINT NOT NULL,
    scale_discount_id BIGINT,
    combo_id BIGINT,
    gift_id BIGINT,
    detail VARCHAR(200) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    CONSTRAINT ck_qld_amount CHECK (amount >= 0),
    CONSTRAINT ck_qld_one_lever CHECK (
        (scale_discount_id IS NOT NULL AND combo_id IS NULL AND gift_id IS NULL)
        OR (scale_discount_id IS NULL AND combo_id IS NOT NULL AND gift_id IS NULL)
        OR (scale_discount_id IS NULL AND combo_id IS NULL AND gift_id IS NOT NULL)
    ),
    CONSTRAINT fk_qld_quote_line FOREIGN KEY (quote_line_id) REFERENCES quote_line (id),
    CONSTRAINT fk_qld_scale FOREIGN KEY (scale_discount_id) REFERENCES scale_discount (id),
    CONSTRAINT fk_qld_combo FOREIGN KEY (combo_id) REFERENCES combo (id),
    CONSTRAINT fk_qld_gift FOREIGN KEY (gift_id) REFERENCES gift (id)
);
CREATE INDEX ix_qld_quote_line_id ON quote_line_discount (quote_line_id);
CREATE INDEX ix_qld_scale_discount_id ON quote_line_discount (scale_discount_id);
CREATE INDEX ix_qld_combo_id ON quote_line_discount (combo_id);
CREATE INDEX ix_qld_gift_id ON quote_line_discount (gift_id);

CREATE TABLE quote_gift (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    quote_id BIGINT NOT NULL,
    gift_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    qty INTEGER NOT NULL,
    taxable BOOLEAN NOT NULL,
    gift_value DECIMAL(19,4) NOT NULL,
    tax DECIMAL(19,4) NOT NULL,
    CONSTRAINT ck_quote_gift_qty CHECK (qty > 0),
    CONSTRAINT ck_quote_gift_value CHECK (gift_value >= 0),
    CONSTRAINT ck_quote_gift_tax CHECK (tax >= 0),
    CONSTRAINT fk_quote_gift_quote FOREIGN KEY (quote_id) REFERENCES quote (id),
    CONSTRAINT fk_quote_gift_gift FOREIGN KEY (gift_id) REFERENCES gift (id),
    CONSTRAINT fk_quote_gift_product FOREIGN KEY (product_id) REFERENCES product (id)
);
CREATE INDEX ix_quote_gift_quote_id ON quote_gift (quote_id);
CREATE INDEX ix_quote_gift_gift_id ON quote_gift (gift_id);
CREATE INDEX ix_quote_gift_product_id ON quote_gift (product_id);
