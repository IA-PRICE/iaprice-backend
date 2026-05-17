-- V3__alter_products.sql
-- Adaptation colonnes table products (picture_url, product_url, suppression anciens champs)

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS picture_url  VARCHAR(500),
    ADD COLUMN IF NOT EXISTS product_url  VARCHAR(500);

ALTER TABLE products
    RENAME COLUMN my_price TO price;

ALTER TABLE products
DROP COLUMN IF EXISTS min_price,
    DROP COLUMN IF EXISTS max_price,
    DROP COLUMN IF EXISTS stock_status,
    DROP COLUMN IF EXISTS attributes;
