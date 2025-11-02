CREATE TABLE IF NOT EXISTS beneficiaries
(
    id        BIGSERIAL PRIMARY KEY,
    iban      VARCHAR UNIQUE,
    swift     VARCHAR,
    name      VARCHAR,
    card      VARCHAR UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT requisites
        CHECK ((IBAN IS NOT NULL AND SWIFT IS NULL) OR card IS NOT NULL)
);
