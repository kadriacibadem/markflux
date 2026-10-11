CREATE TABLE orders
(
    id              UUID PRIMARY KEY,
    user_id         UUID           NOT NULL,
    idempotency_key UUID           NOT NULL,
    status          VARCHAR(20)    NOT NULL,
    total_amount    NUMERIC(14, 2) NOT NULL,
    currency        VARCHAR(3)     NOT NULL,
    failure_reason  VARCHAR(500),
    version         BIGINT         NOT NULL,
    created_at      TIMESTAMPTZ    NOT NULL,
    updated_at      TIMESTAMPTZ    NOT NULL,
    UNIQUE (user_id, idempotency_key)
);

CREATE INDEX idx_orders_user_created ON orders (user_id, created_at DESC);
CREATE INDEX idx_orders_pending ON orders (created_at) WHERE status = 'PENDING';

CREATE TABLE order_item
(
    id           UUID PRIMARY KEY,
    order_id     UUID           NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id   UUID           NOT NULL,
    product_name VARCHAR(200)   NOT NULL,
    unit_price   NUMERIC(12, 2) NOT NULL,
    quantity     INTEGER        NOT NULL CHECK (quantity > 0),
    line_total   NUMERIC(14, 2) NOT NULL
);

CREATE INDEX idx_order_item_order ON order_item (order_id);

CREATE TABLE order_status_history
(
    id         UUID PRIMARY KEY,
    order_id   UUID        NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    reason     VARCHAR(500),
    changed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_order_status_history_order ON order_status_history (order_id);
