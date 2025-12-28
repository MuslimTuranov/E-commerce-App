-- V1__create_orders_table.sql

CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(255) NOT NULL UNIQUE,
    sku_code VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    price NUMERIC(19,2) NOT NULL
);

-- Optional indexes for faster queries
CREATE INDEX IF NOT EXISTS idx_order_order_number ON orders(order_number);
CREATE INDEX IF NOT EXISTS idx_order_sku_code ON orders(sku_code);
