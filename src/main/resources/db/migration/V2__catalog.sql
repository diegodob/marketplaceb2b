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
