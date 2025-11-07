CREATE TABLE IF NOT EXISTS shipments
(
    id                     BIGSERIAL PRIMARY KEY,
    code                   VARCHAR                  NOT NULL UNIQUE,
    warehouse_id_sender    BIGINT REFERENCES warehouses (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    warehouse_id_recipient BIGINT REFERENCES warehouses (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    address                JSONB,
    stock_item_id          BIGINT                   NOT NULL REFERENCES stock_items (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    stock_item_quantity    BIGINT                   NOT NULL CHECK (stock_item_quantity >= 0),
    initiator_id           BIGINT                   NOT NULL REFERENCES users (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    status                 VARCHAR                  NOT NULL,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    shipment_direction     VARCHAR                  NOT NULL,
    CONSTRAINT nullable_address CHECK (address IS NOT NULL OR warehouse_id_sender IS NOT NULL OR
                                       warehouse_id_recipient IS NOT NULL)
)