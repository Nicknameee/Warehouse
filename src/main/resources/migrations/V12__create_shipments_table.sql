CREATE TABLE IF NOT EXISTS shipments
(
    id                     BIGSERIAL PRIMARY KEY,
    warehouse_id_sender    BIGINT  NOT NULL REFERENCES warehouses (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    warehouse_id_recipient BIGINT REFERENCES warehouses (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    address                JSONB,
    stock_item_id          BIGINT  NULL REFERENCES stock_items (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    stock_item_quantity    BIGINT  NULL CHECK (stock_item_quantity >= 0),
    initiator_id           BIGINT  NOT NULL REFERENCES users (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    status                 VARCHAR NOT NULL
)