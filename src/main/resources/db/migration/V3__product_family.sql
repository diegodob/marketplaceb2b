CREATE TABLE product_family (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    CONSTRAINT uq_product_family_name UNIQUE (tenant_id, name),
    CONSTRAINT fk_product_family_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);

ALTER TABLE product ADD COLUMN family_id BIGINT;
ALTER TABLE product ADD CONSTRAINT fk_product_family FOREIGN KEY (family_id) REFERENCES product_family (id);
CREATE INDEX ix_product_family_id ON product (family_id);

ALTER TABLE product DROP COLUMN family;
