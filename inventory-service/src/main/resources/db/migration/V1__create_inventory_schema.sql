CREATE TABLE inventory
(
    product_id        UUID PRIMARY KEY,
    quantity          INTEGER     NOT NULL CHECK (quantity >= 0),
    reserved_quantity INTEGER     NOT NULL CHECK (reserved_quantity >= 0),
    version           BIGINT      NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL,
    CHECK (reserved_quantity <= quantity)
);

CREATE TABLE stock_reservation
(
    id         UUID PRIMARY KEY,
    order_id   UUID        NOT NULL UNIQUE,
    status     VARCHAR(20) NOT NULL CHECK (status IN ('RESERVED', 'CONFIRMED', 'RELEASED', 'REJECTED')),
    expires_at TIMESTAMPTZ,
    reason     VARCHAR(500),
    version    BIGINT      NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE stock_reservation_line
(
    reservation_id UUID    NOT NULL REFERENCES stock_reservation (id) ON DELETE CASCADE,
    product_id     UUID    NOT NULL,
    quantity       INTEGER NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (reservation_id, product_id)
);

CREATE TABLE stock_movement
(
    id              UUID PRIMARY KEY,
    product_id      UUID        NOT NULL REFERENCES inventory (product_id),
    quantity_change INTEGER     NOT NULL,
    type            VARCHAR(30) NOT NULL,
    reference_id    VARCHAR(100),
    reason          VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_stock_movement_product ON stock_movement (product_id, created_at DESC);
