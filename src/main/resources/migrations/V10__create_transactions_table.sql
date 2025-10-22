CREATE TABLE IF NOT EXISTS transactions
(
    id                  BIGSERIAL PRIMARY KEY,
    transaction_id      VARCHAR UNIQUE              NOT NULL,
    reference           VARCHAR UNIQUE              NOT NULL,
    flow_type           VARCHAR                     NOT NULL,
    purpose             VARCHAR                     NOT NULL,
    status              VARCHAR                     NOT NULL,
    amount              NUMERIC(19, 2)              NOT NULL,
    currency            VARCHAR                     NOT NULL,
    payer               JSON                        NOT NULL,
    receiver            JSON                        NOT NULL,
    metadata            JSON                        NOT NULL,
    fee                 JSON                        NOT NULL,
    external_references JSON                        NOT NULL,
    created_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITHOUT TIME ZONE,
    paid_at             TIMESTAMP WITHOUT TIME ZONE,
    payment_provider    VARCHAR                     NOT NULL
)