CREATE TABLE IF NOT EXISTS stock_item_logs
(
    id                              BIGSERIAL PRIMARY KEY,
    event_id                        VARCHAR UNIQUE              NOT NULL,
    initiator_id                    BIGINT                      NOT NULL REFERENCES users (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    transaction_id                  BIGINT REFERENCES transactions (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    type                            VARCHAR                     NOT NULL,
    action_initiator_scope          VARCHAR,
    quantity_of_items               BIGINT                      NOT NULL CHECK (quantity_of_items >= 0),
    available_quantity_before_event BIGINT                      NOT NULL CHECK (available_quantity_before_event >= 0),
    available_quantity_after_event  BIGINT                      NOT NULL CHECK (available_quantity_after_event >= 0),
    reserved_quantity_before        BIGINT                      NOT NULL CHECK (reserved_quantity_before >= 0),
    reserved_quantity_after         BIGINT                      NOT NULL CHECK (reserved_quantity_after >= 0),
    stock_item_id                   BIGINT                      NOT NULL REFERENCES stock_items (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    warehouse_id                    BIGINT                      NOT NULL REFERENCES warehouses (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    shipment_id                     BIGINT                      NOT NULL REFERENCES shipments (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    description                     VARCHAR,
    occurred_at                     TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    metadata                        JSONB
)