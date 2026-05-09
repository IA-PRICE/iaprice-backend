-- ============================================================
-- V1__init_auth.sql
-- Module 1 — Auth + Multi-tenant
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ── PLANS ────────────────────────────────────────────────────
CREATE TABLE plans (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name             VARCHAR(50)    NOT NULL,
    price_mad        DECIMAL(10,2)  NOT NULL DEFAULT 0,
    product_limit    INT            NOT NULL DEFAULT 10,
    competitor_limit INT            NOT NULL DEFAULT 3,
    search_limit     INT            NOT NULL DEFAULT 10,
    has_alerts       BOOLEAN        NOT NULL DEFAULT FALSE,
    has_reports      BOOLEAN        NOT NULL DEFAULT FALSE,
    has_repricing    BOOLEAN        NOT NULL DEFAULT FALSE,
    has_autopilot    BOOLEAN        NOT NULL DEFAULT FALSE,
    has_api          BOOLEAN        NOT NULL DEFAULT FALSE,
    features         JSONB,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

INSERT INTO plans (name, price_mad, product_limit, competitor_limit, search_limit)
VALUES
    ('free',    0,    10,   3,   10),
    ('starter', 199,  100,  10,  100),
    ('pro',     499,  1000, 50,  -1),
    ('elite',   999,  -1,   -1,  -1);

-- ── ORGANIZATIONS ────────────────────────────────────────────
CREATE TABLE organizations (
    id          UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(100) UNIQUE NOT NULL,
    sector      VARCHAR(100),
    country     VARCHAR(10)  DEFAULT 'MA',
    currency    VARCHAR(10)  DEFAULT 'MAD',
    settings    JSONB,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ── USERS ────────────────────────────────────────────────────
CREATE TABLE users (
    id              UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    email           VARCHAR(255) UNIQUE NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ── ORG_MEMBERS ──────────────────────────────────────────────
CREATE TABLE org_members (
    id          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id      UUID        NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role        VARCHAR(50) NOT NULL DEFAULT 'owner',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(org_id, user_id)
);

-- ── SUBSCRIPTIONS ────────────────────────────────────────────
CREATE TABLE subscriptions (
    id           UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id       UUID        NOT NULL REFERENCES organizations(id),
    plan_id      UUID        NOT NULL REFERENCES plans(id),
    status       VARCHAR(50) NOT NULL DEFAULT 'active',
    searches_used INT        NOT NULL DEFAULT 0,
    period_start TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    period_end   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── INDEX utiles ─────────────────────────────────────────────
CREATE INDEX idx_org_members_org_id  ON org_members(org_id);
CREATE INDEX idx_org_members_user_id ON org_members(user_id);
CREATE INDEX idx_subscriptions_org_id ON subscriptions(org_id);
