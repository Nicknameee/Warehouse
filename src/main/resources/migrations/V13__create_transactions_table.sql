CREATE TABLE IF NOT EXISTS transactions
(
    id                  BIGSERIAL PRIMARY KEY,
    transaction_id      VARCHAR UNIQUE              NOT NULL,
    reference           VARCHAR UNIQUE              NOT NULL,
    flow_type           VARCHAR                     NOT NULL,
    purpose             VARCHAR                     NOT NULL,
    status              VARCHAR                     NOT NULL,
    amount              DECIMAL                     NOT NULL,
    currency            VARCHAR                     NOT NULL,
    beneficiary_id      BIGINT                      NOT NULL REFERENCES beneficiaries (id),
    external_references JSON,
    created_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    paid_at             TIMESTAMP WITHOUT TIME ZONE,
    payment_provider    VARCHAR                     NOT NULL
)