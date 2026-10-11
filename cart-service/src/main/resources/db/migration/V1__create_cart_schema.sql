CREATE TABLE cart
(
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE cart_item
(
    id           UUID PRIMARY KEY,
    cart_id      UUID           NOT NULL REFERENCES cart (id) ON DELETE CASCADE,
    product_id   UUID           NOT NULL,
    product_name VARCHAR(200)   NOT NULL,
    unit_price   NUMERIC(12, 2) NOT NULL,
    quantity     INTEGER        NOT NULL CHECK (quantity BETWEEN 1 AND 99),
    available    BOOLEAN        NOT NULL DEFAULT TRUE,
    added_at     TIMESTAMPTZ    NOT NULL,
    UNIQUE (cart_id, product_id)
);

CREATE INDEX idx_cart_item_product ON cart_item (product_id);
