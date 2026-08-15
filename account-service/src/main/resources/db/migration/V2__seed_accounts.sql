-- Seed the demo user's two accounts with fixed ids + opening balances.
-- Single source of truth: common DemoConstants. Amounts are plain numerics.
INSERT INTO accounts (account_id, user_id, type, balance)
VALUES ('acc-checking-0001', 'demo@bank.dev', 'CHECKING', 1000.00),
       ('acc-savings-0002',  'demo@bank.dev', 'SAVINGS',  500.00)
ON CONFLICT (account_id) DO NOTHING;
