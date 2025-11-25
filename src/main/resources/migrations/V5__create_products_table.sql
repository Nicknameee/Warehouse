CREATE TABLE IF NOT EXISTS products
(
    id          BIGSERIAL PRIMARY KEY NOT NULL,
    code        VARCHAR UNIQUE        NOT NULL,
    title       VARCHAR               NOT NULL,
    description VARCHAR,
    price       BIGINT                NOT NULL,
    currency    VARCHAR               NOT NULL,
    created_at  TIMESTAMPTZ           NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ           NOT NULL DEFAULT NOW(),
    weight BIGINT,
    length BIGINT,
    width  BIGINT,
    height BIGINT
)