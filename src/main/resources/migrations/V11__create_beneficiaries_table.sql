CREATE TABLE IF NOT EXISTS beneficiaries
(
    id        BIGSERIAL PRIMARY KEY,
    iban      VARCHAR UNIQUE NOT NULL,
    swift     VARCHAR NOT NULL,
    name      VARCHAR,
    card      VARCHAR UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
)