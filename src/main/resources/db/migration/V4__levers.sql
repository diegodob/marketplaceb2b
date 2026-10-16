CREATE TABLE scale_discount (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ruleset_id BIGINT NOT NULL,
    target_type VARCHAR(10) NOT NULL,
    product_id BIGINT,
    family_id BIGINT,
    CONSTRAINT ck_scale_target_type CHECK (target_type IN ('SKU', 'FAMILY')),
    CONSTRAINT ck_scale_target_one CHECK (
        (target_type = 'SKU' AND product_id IS NOT NULL AND family_id IS NULL)
        OR (target_type = 'FAMILY' AND family_id IS NOT NULL AND product_id IS NULL)
    ),
    CONSTRAINT fk_scale_ruleset FOREIGN KEY (ruleset_id) REFERENCES ruleset (id),
    CONSTRAINT fk_scale_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT fk_scale_family FOREIGN KEY (family_id) REFERENCES product_family (id)
);
CREATE INDEX ix_scale_discount_ruleset_id ON scale_discount (ruleset_id);
CREATE INDEX ix_scale_discount_product_id ON scale_discount (product_id);
CREATE INDEX ix_scale_discount_family_id ON scale_discount (family_id);

CREATE TABLE scale_tier (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    scale_discount_id BIGINT NOT NULL,
    from_qty INTEGER NOT NULL,
    percentage DECIMAL(7,4) NOT NULL,
    CONSTRAINT ck_scale_tier_from_qty CHECK (from_qty > 0),
    CONSTRAINT ck_scale_tier_percentage CHECK (percentage >= 0 AND percentage <= 100),
    CONSTRAINT fk_scale_tier_discount FOREIGN KEY (scale_discount_id) REFERENCES scale_discount (id)
);
CREATE INDEX ix_scale_tier_discount_id ON scale_tier (scale_discount_id);

CREATE TABLE combo (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ruleset_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    combo_price DECIMAL(19,4) NOT NULL,
    CONSTRAINT ck_combo_price CHECK (combo_price >= 0),
    CONSTRAINT fk_combo_ruleset FOREIGN KEY (ruleset_id) REFERENCES ruleset (id)
);
CREATE INDEX ix_combo_ruleset_id ON combo (ruleset_id);

CREATE TABLE combo_item (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    combo_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    min_qty INTEGER NOT NULL,
    CONSTRAINT uq_combo_item UNIQUE (combo_id, product_id),
    CONSTRAINT ck_combo_item_min_qty CHECK (min_qty > 0),
    CONSTRAINT fk_combo_item_combo FOREIGN KEY (combo_id) REFERENCES combo (id),
    CONSTRAINT fk_combo_item_product FOREIGN KEY (product_id) REFERENCES product (id)
);
CREATE INDEX ix_combo_item_product_id ON combo_item (product_id);

CREATE TABLE gift_condition (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id BIGINT,
    family_id BIGINT,
    min_qty INTEGER,
    min_amount DECIMAL(19,4),
    CONSTRAINT ck_gift_condition_target CHECK (NOT (product_id IS NOT NULL AND family_id IS NOT NULL)),
    CONSTRAINT ck_gift_condition_threshold CHECK (min_qty IS NOT NULL OR min_amount IS NOT NULL),
    CONSTRAINT ck_gift_condition_qty CHECK (min_qty IS NULL OR min_qty > 0),
    CONSTRAINT ck_gift_condition_amount CHECK (min_amount IS NULL OR min_amount >= 0),
    CONSTRAINT ck_gift_condition_qty_target CHECK (min_qty IS NULL OR product_id IS NOT NULL OR family_id IS NOT NULL),
    CONSTRAINT fk_gift_condition_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT fk_gift_condition_family FOREIGN KEY (family_id) REFERENCES product_family (id)
);
CREATE INDEX ix_gift_condition_product_id ON gift_condition (product_id);
CREATE INDEX ix_gift_condition_family_id ON gift_condition (family_id);

CREATE TABLE gift (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ruleset_id BIGINT NOT NULL,
    condition_id BIGINT NOT NULL,
    gift_product_id BIGINT NOT NULL,
    gift_qty INTEGER NOT NULL,
    CONSTRAINT ck_gift_qty CHECK (gift_qty > 0),
    CONSTRAINT fk_gift_ruleset FOREIGN KEY (ruleset_id) REFERENCES ruleset (id),
    CONSTRAINT fk_gift_condition FOREIGN KEY (condition_id) REFERENCES gift_condition (id),
    CONSTRAINT fk_gift_product FOREIGN KEY (gift_product_id) REFERENCES product (id)
);
CREATE INDEX ix_gift_ruleset_id ON gift (ruleset_id);
CREATE INDEX ix_gift_condition_id ON gift (condition_id);
CREATE INDEX ix_gift_product_id ON gift (gift_product_id);
