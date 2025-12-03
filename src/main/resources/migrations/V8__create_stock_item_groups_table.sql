CREATE TABLE IF NOT EXISTS stock_item_groups
(
    id        BIGSERIAL PRIMARY KEY NOT NULL,
    code      VARCHAR UNIQUE        NOT NULL,
    name VARCHAR UNIQUE NOT NULL,
    is_active BOOLEAN               NOT NULL
)