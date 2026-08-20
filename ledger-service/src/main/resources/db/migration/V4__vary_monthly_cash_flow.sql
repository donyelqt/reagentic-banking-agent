-- Story 3b (insight arc): vary the monthly cash flow so the dashboard's
-- Net cash flow chart reads as a real story.
--
-- V3 seeded 12 monthly cycles (Aug 2025 - Jul 2026) that each net exactly
-- zero (+3200.00 payroll, -3200.00 expenses), which renders as a flat $0
-- line in the chart. This migration replaces those rows with cycles that
-- carry a monthly surplus/deficit (-350 to +300: deficits around the
-- holidays, surpluses in spring). The deltas sum to exactly zero, so the
-- balance_after chain is self-consistent by construction: it starts at the
-- opening 1000.00 and ends exactly on the seeded account balance 1000.00 -
-- reconciliation stays green.
--
-- Seeded rows are identified by a non-null description (live transfers
-- carry none, per the V3 note), so user-created transfers are left
-- untouched: they were appended after the seeded chain and still chain from
-- the same 1000.00 ending balance.

DELETE FROM ledger_entries
WHERE account_id = 'acc-checking-0001' AND description IS NOT NULL;

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
months(m, delta, extra_type, extra_desc) AS (
    VALUES
        (0,  150.00, 'CREDIT', 'Freelance Design Gig'),
        (1, -200.00, 'DEBIT',  'Back to School Supplies'),
        (2,  100.00, 'CREDIT', 'Garage Sale Earnings'),
        (3, -250.00, 'DEBIT',  'Holiday Gift Shopping'),
        (4, -350.00, 'DEBIT',  'Holiday Travel'),
        (5,  200.00, 'CREDIT', 'Quarterly Bonus'),
        (6,  150.00, 'CREDIT', 'Freelance Design Gig'),
        (7, -100.00, 'DEBIT',  'Home Repairs'),
        (8,  250.00, 'CREDIT', 'Tax Refund'),
        (9,  300.00, 'CREDIT', 'Freelance Design Gig'),
        (10,-150.00, 'DEBIT',  'Summer Trip Booking'),
        (11,-100.00, 'DEBIT',  'Seasonal Clothes Shopping')
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
    UNION ALL
    SELECT m, 26,
           'acc-checking-0001' AS account_id,
           extra_type, extra_desc, delta,
           (EXTRACT(EPOCH FROM (
               TIMESTAMP '2025-08-01 00:00:00+00'
               + (m || ' months')::interval
               + '26 days'::interval
           )))::bigint * 1000 AS created_at
    FROM months
)
SELECT account_id, NULL, type, description, signed,
       1000.00 + SUM(signed) OVER (
           ORDER BY m, seq ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
       ) AS balance_after,
       created_at
FROM rows;