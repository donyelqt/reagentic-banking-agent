# Demo Runbook (Aug 20)

Prereqs: `docker compose up --build` healthy (all `/actuator/health` up),
frontend on `http://localhost:5173`, demo user seeded.

## 1. Login
```
email: demo@bank.dev
password: demo1234
```
Dashboard shows Checking $1000.00 / Savings $500.00.

## 2. Manual transfer (spine demo)
In the UI Transfer tab, or:
```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"demo@bank.dev","password":"demo1234"}' | jq -r .data.token)
curl -s -X POST localhost:8080/api/payments/transfer -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"sourceAccountId":"acc-checking-0001","destinationAccountId":"acc-savings-0002","amount":"50.00","idempotencyKey":"demo-001"}' | jq
```
Checking → $950.00, Savings → $550.00, ledger + notification updated.

## 3. Agent transfer (guardrail)
Agent chat: `transfer 50 to savings` → agent returns `pendingSteps` (approval
required) → frontend shows the confirmation modal → **Approve** → transfer
executes with the same idempotency key.

## 4. Reconcile (clean)
Agent chat: `reconcile my checking account` → `BALANCED`.

## 5. Hero break (reconciliation)
```bash
# stop ledger, inject fault, restart
docker compose stop ledger-service
LEDGER_FAULT_SKIP_APPEND=true docker compose up -d ledger-service
```
Do a transfer, then reconcile the source account → agent reports
**MISMATCH** with `suspect=MISSING_DEBIT_LEG` and the delta — explaining the
missing ledger leg. (Reset by clearing the fault and re-seeding.)

## Reset
```bash
docker compose down -v   # drops volumes; re-seeds on next up
```
