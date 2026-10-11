CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE category
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL
);

CREATE UNIQUE INDEX uk_category_name ON category (lower(name));

CREATE TABLE product
(
    id          UUID PRIMARY KEY,
    sku         VARCHAR(64)    NOT NULL UNIQUE,
    name        VARCHAR(200)   NOT NULL,
    description VARCHAR(4000),
    price       NUMERIC(12, 2) NOT NULL CHECK (price > 0),
    category_id UUID REFERENCES category (id),
    status      VARCHAR(20)    NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    version     BIGINT         NOT NULL,
    created_at  TIMESTAMPTZ    NOT NULL,
    updated_at  TIMESTAMPTZ    NOT NULL
);

CREATE INDEX idx_product_category ON product (category_id);
CREATE INDEX idx_product_status_price ON product (status, price);
CREATE INDEX idx_product_name_trgm ON product USING gin (lower(name) gin_trgm_ops);
