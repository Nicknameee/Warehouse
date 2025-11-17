CREATE TABLE IF NOT EXISTS stock_items
(
    id                 BIGSERIAL PRIMARY KEY NOT NULL,
    batch_version      BIGINT                NOT NULL,
    code               VARCHAR               NOT NULL UNIQUE,
    product_id         BIGINT                NOT NULL REFERENCES products (id),
    group_id           BIGINT                NOT NULL REFERENCES stock_item_groups (id),
    warehouse_id       BIGINT                NOT NULL REFERENCES warehouses (id),
    expiry_date        DATE,
    available_quantity BIGINT                NOT NULL DEFAULT 0 CHECK ( available_quantity >= 0 ),
    status             VARCHAR               NOT NULL,
    is_active          BOOLEAN               NOT NULL,
    storage_section_id BIGINT REFERENCES storage_sections (id),
    CONSTRAINT stock_item_unique_batch UNIQUE (batch_version, product_id, warehouse_id)
)