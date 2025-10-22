CREATE TABLE tags
(
    id        BIGSERIAL PRIMARY KEY NOT NULL,
    name      VARCHAR UNIQUE        NOT NULL,
    is_active BOOLEAN               NOT NULL
);