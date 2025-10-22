CREATE TABLE users
(
    id          BIGSERIAL PRIMARY KEY NOT NULL,
    username    VARCHAR UNIQUE        NOT NULL,
    password    VARCHAR               NOT NULL,
    email       VARCHAR UNIQUE        NOT NULL,
    login_time  TIMESTAMP WITHOUT TIME ZONE,
    logout_time TIMESTAMP WITHOUT TIME ZONE,
    role        VARCHAR               NOT NULL,
    status      VARCHAR               NOT NULL,
    timezone    VARCHAR               NOT NULL
)