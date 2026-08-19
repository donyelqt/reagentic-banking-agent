-- Story 3 (insight arc): realistic demo history + merchant descriptions.
--
-- 1. Adds a merchant description column so ledger entries can be classified by
--    /api/agent/classify (the classify contract is (description, amount)).
--    Live transfers carry no merchant name (PaymentCompletedEvent has none), so
--    their description stays NULL and they are excluded from spending analysis
--    client-side - transfers are not spending.
--
-- 2. Moves the V1 opening entries off the epoch (created_at=0 rendered as
--    "Jan 1" on every chart axis) to 2025-07-15, so the ledger reads as a
--    continuous 13-month story ending at the current balance.
--
-- 3. Seeds 12 monthly cycles (Aug 2025 - Jul 2026) of realistic categorized
--    transactions for acc-checking-0001. Every month nets exactly zero
--    (+3200.00 payroll, -3200.00 expenses), so the balance_after chain is
--    self-consistent by construction: it starts at the opening 1000.00 and
--    ends exactly on the seeded account balance 1000.00 - reconciliation
--    stays green. acc-savings-0002 keeps its opening entry only (500.00).
--
-- NOTE: for a fully deterministic demo, run on fresh volumes (docker compose
-- down -v); live transfer entries created after Jul 2026 would sit on top of
-- the seeded chain and shift the final balance.

ALTER TABLE ledger_entries ADD COLUMN IF NOT EXISTS description VARCHAR(255);

UPDATE ledger_entries
SET created_at = (EXTRACT(EPOCH FROM TIMESTAMP '2025-07-15 00:00:00+00'))::bigint * 1000
WHERE type = 'OPENING' AND payment_id IS NULL
  AND account_id IN ('acc-checking-0001', 'acc-savings-0002');

INSERT INTO ledger_entries (account_id, payment_id, type, description, signed_amount, balance_after, created_at)
WITH pattern(seq, day, type, description, signed) AS (
    VALUES
        (1,  1,  'CREDIT', 'ACME Corp Payroll',         3200.00),
        (2,  2,  'DEBIT',  'Metro MRT Pass',            -45.00),
        (3,  3,  'DEBIT',  'Crestview Apartments Rent', -1200.00),
        (4,  3,  'DEBIT',  'Fresh Market Grocery',      -245.60),
        (5,  5,  'DEBIT',  'Cafe Lumen',                -68.50),
        (6,  7,  'DEBIT',  'City Electric & Water',     -165.30),
        (7,  8,  'DEBIT',  'Netflix Monthly',           -15.99),
        (8,  9,  'DEBIT',  'Grab Ride',                 -28.40),
        (9,  9,  'DEBIT',  'Spotify Premium',           -11.99),
        (10, 10, 'DEBIT',  'iCloud Storage',            -9.99),
        (11, 12, 'DEBIT',  'Sakura Sushi Restaurant',   -112.75),
        (12, 13, 'DEBIT',  'Auto Insurance',            -185.00),
        (13, 14, 'DEBIT',  'Nordhaus Apparel Store',    -129.99),
        (14, 15, 'DEBIT',  'Gym & Fitness',             -55.00),
        (15, 16, 'DEBIT',  'Mobile & Internet',         -130.00),
        (16, 17, 'DEBIT',  'CornerMart Groceries',      -183.20),
        (17, 18, 'DEBIT',  'PharmaCare Pharmacy',       -42.00),
        (18, 20, 'DEBIT',  'CinePlex Cinema',           -35.00),
        (19, 21, 'DEBIT',  'Aqua Utility Billing',      -52.10),
        (20, 22, 'DEBIT',  'The Griddle Restaurant',    -94.30),
        (21, 26, 'DEBIT',  'KitchenWares Store',        -74.50),
        (22, 26, 'DEBIT',  'Corner Coffee',             -93.50),
        (23, 26, 'DEBIT',  'Gift Shopping',             -101.89),
        (24, 26, 'DEBIT',  'Hair Studio',               -60.00),
        (25, 26, 'DEBIT',  'Home Goods',                -60.00)
),
months(m) AS (
    SELECT generate_series(0, 11)
),
rows AS (
    SELECT m, seq,
           'acc-checking-0001' AS account_id,
           type, description, signed,
           (EXTRACT(EPOCH FROM (
               TIMESTAMP '2025-08-01 00:00:00+00'
               + (m || ' months')::interval
               + ((day - 1) || ' days')::interval
           )))::bigint * 1000 AS created_at
    FROM pattern CROSS JOIN months
)
SELECT account_id, NULL, type, description, signed,
       1000.00 + SUM(signed) OVER (
           ORDER BY m, seq ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
       ) AS balance_after,
       created_at
FROM rows;