CREATE TABLE stock_items
(
    id                 BIGSERIAL PRIMARY KEY NOT NULL,
    product_id         BIGINT                NOT NULL REFERENCES products (id),
    group_id           BIGINT                NOT NULL REFERENCES stock_item_groups (id),
    warehouse_id       BIGINT                NOT NULL REFERENCES warehouses (id),
    expiry_date        DATE,
    available_quantity BIGINT                NOT NULL DEFAULT 0 CHECK ( available_quantity >= 0 ),
    reserved_quantity  BIGINT                NOT NULL DEFAULT 0 CHECK ( reserved_quantity >= 0 ),
    status             VARCHAR               NOT NULL
)