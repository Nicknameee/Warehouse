CREATE TABLE IF NOT EXISTS storage_sections
(
    id           SERIAL PRIMARY KEY,
    warehouse_id INT          NOT NULL REFERENCES warehouses (id),
    code         VARCHAR(255) NOT NULL,
    CONSTRAINT storage_sections_code_unique UNIQUE (warehouse_id, code)
)