CREATE TABLE IF NOT EXISTS beneficiaries
(
    id    BIGSERIAL PRIMARY KEY,
    code  VARCHAR UNIQUE NOT NULL,
    iban  VARCHAR        NOT NULL,
    swift VARCHAR,
    name  VARCHAR,
    card  VARCHAR
);
