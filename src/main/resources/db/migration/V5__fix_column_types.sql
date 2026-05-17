ALTER TABLE products
ALTER COLUMN name    TYPE VARCHAR(255) USING name::text,
    ALTER COLUMN brand   TYPE VARCHAR(100) USING brand::text,
    ALTER COLUMN ean     TYPE VARCHAR(50)  USING ean::text,
    ALTER COLUMN sku     TYPE VARCHAR(100) USING sku::text,
    ALTER COLUMN picture_url TYPE VARCHAR(500) USING picture_url::text,
    ALTER COLUMN product_url TYPE VARCHAR(500) USING product_url::text;