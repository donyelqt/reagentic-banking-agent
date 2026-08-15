# Architecture

## Topology
```
Browser ──▶ gateway :8080 (JWT verify, CORS, route)
                ├─ /api/auth/**        → auth-service :8081
                ├─ /api/accounts/**     → account-service :8082
                ├─ /api/payments/**     → payment-service :8083
                ├─ /api/ledger/**       → ledger-service :8084
                ├─ /api/notifications/**→ notification-service :8085
                └─ /api/agent/**        → ai-agent :8086
```

The `ai-agent` calls the backend **directly** (service-to-service) via
`*_SERVICE_URL`, copying the caller's `Authorization` header. The gateway only
fronts the external frontend→agent hop. Every service re-verifies the JWT
(**zero internal trust**).

## Data flow — a transfer
1. `payment-service` runs the saga: `debit(source)` → `credit(dest)`, each with a
   **distinct** `idempotencyKey`; on credit failure it compensates (credits
   source back).
2. On success it writes a `PaymentCompleted` outbox row (same local DB tx),
   relayed to Kafka topic `payment-events` (idempotent, acked after send).
3. `ledger-service` consumes and appends `source -amount` + `dest +amount`
   (idempotent by `paymentId`). `notification-service` consumes and records a
   confirmation.
4. Opening ledger entries are Flyway-seeded from `DemoConstants`, so the
   reconciliation invariant holds at t0 with no Kafka dependency.

## Money & consistency
- `Money` is a `BigDecimal` (scale 2, HALF_UP), serialized as a JSON **string**.
- Ownership enforced at `account-service` (`loadOwned`); `payment-service`
  propagates the caller JWT.
- `LEDGER_FAULT_SKIP_APPEND=true` makes the ledger consume/ack but skip the
  append — the deterministic "missing leg" break for the demo.

## Agent
Deterministic planner + hand-rolled executor (the spec's sanctioned MVP boot
path; works with **no LLM**). Workers are typed REST clients over the real
backend: `listAccounts`, `getBalance`, `listTransactions`, `transferFunds`,
`reconcileAccount`. Mutating steps become `pendingSteps` requiring explicit
approval; the frontend re-calls with the same `plan` + approved `stepIds`.
