CREATE TABLE IF NOT EXISTS product_photos
(
    id                  BIGSERIAL PRIMARY KEY NOT NULL,
    product_id BIGINT NOT NULL REFERENCES products (id),
    photo_url           VARCHAR UNIQUE        NOT NULL,
    external_reference  VARCHAR UNIQUE        NOT NULL,
    external_references JSON
)