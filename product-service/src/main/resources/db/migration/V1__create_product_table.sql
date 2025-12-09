-- V1__create_product_table.sql

CREATE TABLE IF NOT EXISTS product (
    id BIGSERIAL PRIMARY KEY,
    sku_code VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(19,2) NOT NULL,
    quantity INTEGER DEFAULT 0
    );

CREATE INDEX IF NOT EXISTS idx_product_sku_code ON product(sku_code);

CREATE INDEX IF NOT EXISTS idx_product_name ON product(name);