CREATE TABLE IF NOT EXISTS tags
(
    id        BIGSERIAL PRIMARY KEY NOT NULL,
    name      VARCHAR UNIQUE        NOT NULL,
    is_active BOOLEAN               NOT NULL
)