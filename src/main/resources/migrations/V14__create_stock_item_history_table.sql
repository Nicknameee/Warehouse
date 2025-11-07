CREATE TABLE IF NOT EXISTS stock_item_history
(
    id                    BIGSERIAL PRIMARY KEY,
    stock_item_id         BIGINT    NOT NULL REFERENCES stock_items (id),
    current_product_price BIGINT    NOT NULL,
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    old_warehouse_id      BIGINT REFERENCES warehouses (id),
    new_warehouse_id      BIGINT REFERENCES warehouses (id),
    quantity_before       BIGINT CHECK ( quantity_before >= 0 ),
    quantity_after        BIGINT CHECK ( quantity_after >= 0 ),
    old_expiration        DATE,
    new_expiration        DATE,
    old_status            VARCHAR(255),
    new_status            VARCHAR(255),
    old_activity          BOOLEAN,
    new_activity          BOOLEAN
)