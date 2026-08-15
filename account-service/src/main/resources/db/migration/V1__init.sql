CREATE TABLE IF NOT EXISTS accounts (
    account_id VARCHAR(64) PRIMARY KEY,
    user_id     VARCHAR(255) NOT NULL,
    type        VARCHAR(32)  NOT NULL,
    balance     NUMERIC(19,2) NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS account_idempotency (
    id               BIGSERIAL PRIMARY KEY,
    idempotency_key  VARCHAR(255) NOT NULL UNIQUE,
    account_id       VARCHAR(64),
    op               VARCHAR(16),
    amount           NUMERIC(19,2)
);
