CREATE TABLE transactions (
    id                VARCHAR(36) PRIMARY KEY,
    account_id        VARCHAR(64)     NOT NULL,
    transaction_date  DATE            NOT NULL,
    description       VARCHAR(255)    NOT NULL,
    amount            NUMERIC(19, 4)  NOT NULL,
    raw_category      VARCHAR(255),
    category          VARCHAR(32)     NOT NULL DEFAULT 'UNCATEGORIZED',
    upload_batch_id   VARCHAR(36)     NOT NULL,
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_transactions_account_id ON transactions (account_id);
CREATE INDEX idx_transactions_transaction_date ON transactions (transaction_date);
CREATE INDEX idx_transactions_upload_batch_id ON transactions (upload_batch_id);
