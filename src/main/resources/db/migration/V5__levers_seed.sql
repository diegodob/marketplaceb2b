INSERT INTO product_family (tenant_id, name)
SELECT id, 'Gaseosas' FROM tenant WHERE name = 'Bebidas GT';
INSERT INTO product_family (tenant_id, name)
SELECT id, 'Aguas' FROM tenant WHERE name = 'Bebidas GT';

UPDATE product SET family_id = (SELECT id FROM product_family WHERE name = 'Gaseosas')
WHERE sku = 'GAS-500';
UPDATE product SET family_id = (SELECT id FROM product_family WHERE name = 'Aguas')
WHERE sku = 'AGU-500';

INSERT INTO scale_discount (ruleset_id, target_type, product_id, family_id)
SELECT r.id, 'SKU', p.id, NULL
FROM ruleset r, product p
WHERE r.version = 'GT-2026-10-01.1' AND p.sku = 'GAS-500';

INSERT INTO scale_tier (scale_discount_id, from_qty, percentage)
SELECT sd.id, 12, 3.0000 FROM scale_discount sd
WHERE sd.target_type = 'SKU' AND sd.product_id = (SELECT id FROM product WHERE sku = 'GAS-500');
INSERT INTO scale_tier (scale_discount_id, from_qty, percentage)
SELECT sd.id, 24, 7.0000 FROM scale_discount sd
WHERE sd.target_type = 'SKU' AND sd.product_id = (SELECT id FROM product WHERE sku = 'GAS-500');

INSERT INTO scale_discount (ruleset_id, target_type, product_id, family_id)
SELECT r.id, 'FAMILY', NULL, f.id
FROM ruleset r, product_family f
WHERE r.version = 'GT-2026-10-01.1' AND f.name = 'Gaseosas';

INSERT INTO scale_tier (scale_discount_id, from_qty, percentage)
SELECT sd.id, 10, 5.0000 FROM scale_discount sd
WHERE sd.target_type = 'FAMILY' AND sd.family_id = (SELECT id FROM product_family WHERE name = 'Gaseosas');
INSERT INTO scale_tier (scale_discount_id, from_qty, percentage)
SELECT sd.id, 50, 10.0000 FROM scale_discount sd
WHERE sd.target_type = 'FAMILY' AND sd.family_id = (SELECT id FROM product_family WHERE name = 'Gaseosas');

INSERT INTO combo (ruleset_id, name, combo_price)
SELECT id, 'Gaseosa + Agua x6', 20000.0000 FROM ruleset WHERE version = 'GT-2026-10-01.1';

INSERT INTO combo_item (combo_id, product_id, min_qty)
SELECT c.id, p.id, 6 FROM combo c, product p
WHERE c.name = 'Gaseosa + Agua x6' AND p.sku = 'GAS-500';
INSERT INTO combo_item (combo_id, product_id, min_qty)
SELECT c.id, p.id, 6 FROM combo c, product p
WHERE c.name = 'Gaseosa + Agua x6' AND p.sku = 'AGU-500';

INSERT INTO gift_condition (product_id, family_id, min_qty, min_amount)
SELECT id, NULL, 12, NULL FROM product WHERE sku = 'GAS-500';
INSERT INTO gift_condition (product_id, family_id, min_qty, min_amount)
VALUES (NULL, NULL, NULL, 50000.0000);
INSERT INTO gift_condition (product_id, family_id, min_qty, min_amount)
SELECT id, NULL, 12, 50000.0000 FROM product WHERE sku = 'GAS-500';

INSERT INTO gift (ruleset_id, condition_id, gift_product_id, gift_qty)
SELECT r.id, gc.id, gp.id, 1
FROM ruleset r
JOIN gift_condition gc ON gc.min_qty = 12 AND gc.min_amount IS NULL
JOIN product gp ON gp.sku = 'AGU-500'
WHERE r.version = 'GT-2026-10-01.1';

INSERT INTO gift (ruleset_id, condition_id, gift_product_id, gift_qty)
SELECT r.id, gc.id, gp.id, 1
FROM ruleset r
JOIN gift_condition gc ON gc.min_qty IS NULL AND gc.min_amount = 50000.0000
JOIN product gp ON gp.sku = 'AGU-500'
WHERE r.version = 'GT-2026-10-01.1';

INSERT INTO gift (ruleset_id, condition_id, gift_product_id, gift_qty)
SELECT r.id, gc.id, gp.id, 2
FROM ruleset r
JOIN gift_condition gc ON gc.min_qty = 12 AND gc.min_amount = 50000.0000
JOIN product gp ON gp.sku = 'AGU-500'
WHERE r.version = 'GT-2026-10-01.1';
