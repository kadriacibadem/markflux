CREATE INDEX idx_stock_reservation_expires ON stock_reservation (expires_at) WHERE status = 'RESERVED';
