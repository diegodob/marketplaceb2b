INSERT INTO country (code, name) VALUES ('GT', 'Guatemala');

INSERT INTO fiscal_id_type (country_id, code, name)
SELECT id, 'NIT', 'NIT' FROM country WHERE code = 'GT';

INSERT INTO tenant (name, country_id)
SELECT 'Bebidas GT', id FROM country WHERE code = 'GT';

INSERT INTO tax_category (code, name) VALUES ('GENERAL', 'General');
INSERT INTO tax_category (code, name) VALUES ('REDUCED', 'Reducido');

INSERT INTO ruleset (tenant_id, version, currency, currency_scale, rounding_mode, effective_from)
SELECT id, 'GT-2026-10-01.1', 'GTQ', 2, 'HALF_UP', TIMESTAMP '2026-10-01 00:00:00'
FROM tenant WHERE name = 'Bebidas GT';

INSERT INTO tax_rate (ruleset_id, tax_category_id, rate)
SELECT r.id, c.id, CASE c.code WHEN 'GENERAL' THEN 12.0000 ELSE 0.0000 END
FROM ruleset r
CROSS JOIN tax_category c
WHERE r.version = 'GT-2026-10-01.1' AND c.code IN ('GENERAL', 'REDUCED');

INSERT INTO product (tenant_id, sku, name)
SELECT id, 'GAS-500', 'Gaseosa 500ml' FROM tenant WHERE name = 'Bebidas GT';
INSERT INTO product (tenant_id, sku, name)
SELECT id, 'AGU-500', 'Agua pura 500ml' FROM tenant WHERE name = 'Bebidas GT';
INSERT INTO product (tenant_id, sku, name)
SELECT id, 'LEC-1L', 'Leche entera 1L' FROM tenant WHERE name = 'Bebidas GT';

INSERT INTO product_price (ruleset_id, product_id, tax_category_id, unit_price)
SELECT r.id, p.id, c.id,
       CASE p.sku WHEN 'GAS-500' THEN 4000.0000 WHEN 'LEC-1L' THEN 6000.0000 ELSE 2500.0000 END
FROM product p
JOIN tenant t ON t.id = p.tenant_id
JOIN ruleset r ON r.tenant_id = t.id AND r.version = 'GT-2026-10-01.1'
JOIN tax_category c ON c.code = CASE p.sku WHEN 'LEC-1L' THEN 'REDUCED' ELSE 'GENERAL' END
WHERE t.name = 'Bebidas GT';

INSERT INTO payment_method (tenant_id, code, name, uses_credit)
SELECT id, 'CREDIT', 'Credito', TRUE FROM tenant WHERE name = 'Bebidas GT';
INSERT INTO payment_method (tenant_id, code, name, uses_credit)
SELECT id, 'CASH', 'Efectivo', FALSE FROM tenant WHERE name = 'Bebidas GT';

INSERT INTO customer (tenant_id, name, fiscal_id, fiscal_id_type_id)
SELECT t.id, 'Cliente 12', '20304050607', f.id
FROM tenant t
JOIN fiscal_id_type f ON f.code = 'NIT'
WHERE t.name = 'Bebidas GT';

INSERT INTO credit_account (customer_id, currency, credit_limit, available)
SELECT c.id, 'GTQ', 500000.00, 300000.00
FROM customer c
JOIN tenant t ON t.id = c.tenant_id
WHERE t.name = 'Bebidas GT' AND c.name = 'Cliente 12';
