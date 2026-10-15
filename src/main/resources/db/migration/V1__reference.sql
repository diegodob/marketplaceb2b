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
