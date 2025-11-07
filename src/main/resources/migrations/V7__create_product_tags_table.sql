CREATE TABLE IF NOT EXISTS product_tags
(
    id         BIGSERIAL PRIMARY KEY NOT NULL,
    tag_id     BIGINT                NOT NULL REFERENCES tags (id),
    product_id BIGINT                NOT NULL REFERENCES products (id)
)