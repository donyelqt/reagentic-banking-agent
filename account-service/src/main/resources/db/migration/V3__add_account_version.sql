-- Account entity uses @Version (optimistic locking); the accounts table
-- was missing this column. Add it as BIGINT NOT NULL DEFAULT 0 so
-- existing + seeded rows are valid and JPA can manage the version.
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;