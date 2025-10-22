CREATE TABLE warehouses
(
    id            BIGSERIAL PRIMARY KEY NOT NULL,
    code          VARCHAR UNIQUE        NOT NULL,
    name          VARCHAR UNIQUE        NOT NULL,
    address       JSON                  NOT NULL,
    working_hours JSON                  NOT NULL,
    phones        VARCHAR[],
    manager_id    BIGINT                NOT NULL REFERENCES users (id),
    is_active     BOOLEAN               NOT NULL
)