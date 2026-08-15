CREATE TABLE IF NOT EXISTS notifications (
    id         BIGSERIAL PRIMARY KEY,
    payment_id VARCHAR(255),
    type       VARCHAR(64),
    account_id VARCHAR(255),
    amount     VARCHAR(32),
    currency   VARCHAR(8),
    status     VARCHAR(32),
    created_at BIGINT NOT NULL
);
