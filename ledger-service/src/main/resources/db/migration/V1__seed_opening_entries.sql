CREATE TABLE IF NOT EXISTS ledger_entries (
    entry_id   BIGSERIAL PRIMARY KEY,
    account_id VARCHAR(255) NOT NULL,
    payment_id VARCHAR(255),
    type       VARCHAR(255) NOT NULL,
    signed_amount NUMERIC(19, 2) NOT NULL,
    balance_after NUMERIC(19, 2) NOT NULL,
    created_at BIGINT NOT NULL
);

-- Opening ledger entries seeded from shared demo constants so the
-- reconciliation invariant holds at t0 with no Kafka dependency.
INSERT INTO ledger_entries (account_id, payment_id, type, signed_amount, balance_after, created_at)
VALUES ('acc-checking-0001', NULL, 'OPENING', 1000.00, 1000.00, 0);

INSERT INTO ledger_entries (account_id, payment_id, type, signed_amount, balance_after, created_at)
VALUES ('acc-savings-0002', NULL, 'OPENING', 500.00, 500.00, 0);
