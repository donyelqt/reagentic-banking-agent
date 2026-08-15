CREATE TABLE IF NOT EXISTS payments (
    payment_id           VARCHAR(64) PRIMARY KEY,
    source_account_id    VARCHAR(64) NOT NULL,
    destination_account_id VARCHAR(64) NOT NULL,
    amount               NUMERIC(19,2) NOT NULL,
    currency             VARCHAR(8) NOT NULL,
    status               VARCHAR(16) NOT NULL,
    reason               VARCHAR(255),
    created_at           BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS outbox (
    id           BIGSERIAL PRIMARY KEY,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type   VARCHAR(64) NOT NULL,
    payload      TEXT NOT NULL,
    published    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_outbox_unpublished ON outbox (published, id);
