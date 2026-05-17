-- ============================================================
-- V2__init_catalogue.sql
-- Module 2 — Catalogue
-- Tables : products · my_price_history
-- ============================================================

-- ── PRODUCTS ─────────────────────────────────────────────────
CREATE TABLE products (
    id             UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id         UUID         NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,

    -- Identification
    name           VARCHAR(255) NOT NULL,
    brand          VARCHAR(100),
    ean            VARCHAR(50),
    sku            VARCHAR(100),

    -- Tarification (MAD)
    my_price       DECIMAL(10,2) NOT NULL,
    cost           DECIMAL(10,2),
    min_price      DECIMAL(10,2),
    max_price      DECIMAL(10,2),

    -- Stock : 2 valeurs uniquement
    -- in_stock | out_of_stock
    stock_status   VARCHAR(20)  DEFAULT 'in_stock'
                   CHECK (stock_status IN ('in_stock', 'out_of_stock')),

    -- Statut : actif/inactif
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,

    -- Attributs libres (JSON, ex: {"volume": "1L"})
    attributes     JSONB,

    -- Horodatage import (MAX = meta.lastImportedAt)
    imported_at    TIMESTAMPTZ,

    -- Audit
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Contrainte unicité EAN et SKU par organisation
CREATE UNIQUE INDEX idx_products_org_ean
    ON products(org_id, ean)
    WHERE ean IS NOT NULL;

CREATE UNIQUE INDEX idx_products_org_sku
    ON products(org_id, sku)
    WHERE sku IS NOT NULL;

-- Index pour les filtres courants
CREATE INDEX idx_products_org_id      ON products(org_id);
CREATE INDEX idx_products_is_active   ON products(org_id, is_active);
CREATE INDEX idx_products_stock       ON products(org_id, stock_status);
CREATE INDEX idx_products_imported_at ON products(org_id, imported_at DESC);

-- ── MY_PRICE_HISTORY ──────────────────────────────────────────
CREATE TABLE my_price_history (
    id            UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id    UUID        NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    old_price     DECIMAL(10,2),
    new_price     DECIMAL(10,2) NOT NULL,

    -- "import" | "manual" | "autopilot" (MVP : import uniquement)
    change_source VARCHAR(50) NOT NULL DEFAULT 'import',

    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_price_history_product_id ON my_price_history(product_id);
CREATE INDEX idx_price_history_created_at ON my_price_history(created_at DESC);

-- ── Trigger updated_at (products) ─────────────────────────────
-- Met à jour updated_at automatiquement à chaque UPDATE
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ── Données de test MVP (facultatif, commenter en prod) ───────
-- INSERT INTO products (org_id, name, brand, ean, sku, my_price, cost, stock_status)
-- VALUES (
--     '<remplacer_par_org_id>',
--     'Produit de test', 'TestBrand', '1234567890123', 'TEST-001',
--     99.00, 60.00, 'in_stock'
-- );
