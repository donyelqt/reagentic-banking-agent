-- Ledger idempotency at the database level.
--
-- Kafka redelivery (or two consumers racing the existsByPaymentId check) could
-- otherwise append a payment's legs twice, double-counting balances. A payment
-- writes exactly one row per (payment_id, type) leg:
--   completed -> DEBIT (source) + CREDIT (dest)
--   failed    -> DEBIT_FAILED (source) + COMPENSATE (source) when compensated
-- Seeded demo history rows carry NULL payment_id and are excluded by the
-- partial index, so multiple NULL rows remain allowed.

CREATE UNIQUE INDEX IF NOT EXISTS uq_ledger_entries_payment_type
    ON ledger_entries (payment_id, type)
    WHERE payment_id IS NOT NULL;