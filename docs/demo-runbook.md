# Demo Runbook (Aug 20) — Two-Role Access Model

Prereqs: `docker compose up --build` healthy (all `/actuator/health` up),
frontend on `http://localhost:5173`, users seeded (auth-service seeds both on
startup).

## Roles

| Role | Credentials | Sees |
|---|---|---|
| Customer (USER) | `demo@bank.dev` / `demo1234` | Dashboard + Transfer + **Agent** (own accounts only: balances, transactions, supervised transfers with approval) |
| Ops analyst (EMPLOYEE) | `ops@bank.dev` / `ops1234` | Reconciliation Console only (account selector + agent chat) |

## 1. Customer login (spine demo)

```
email: demo@bank.dev
password: demo1234
```

Dashboard shows Checking $1000.00 / Savings $500.00 + live ledger activity.
Transfer tab: move $50 checking → savings (both balances update, ledger +
notification follow).

## 2. Customer agent (supervised transfers)

Agent tab (customer copy): `transfer 50 to savings` → the agent returns
`pendingSteps` → confirmation modal shows the plan (`from`/`to`/`amount`) →
**Approve** → the approval echoes the server-issued `approvalId`; balances on
Dashboard refresh live (approve twice → still one payment: exactly-once).

Ask `what's my balance?` or `show my transactions` — the agent reads the
ownership-scoped endpoints only. Ask to `reconcile` → the agent refuses
("requires the EMPLOYEE role") — a visible proof that customer powers are
scoped.

## 3. Ops login (two-role demo)

Sign out, log in as `ops@bank.dev` / `ops1234`. Header shows "Ops Analyst";
the nav is the **Reconciliation Console**. The console exposes the same
accounts to demonstrate the cross-account read (an ops analyst can investigate
ANY account).

## 4. Reconcile (clean)

Console: pick Checking, send `reconcile` (or `reconcile acc-checking-0001`).
Agent answers **BALANCED** (balance == Σ ledger, verified against the
immutable ledger and account-service).

## 5. Hero break (root-cause diagnosis)

```bash
# stop ledger, inject fault, restart (compose forwards the env var)
docker compose stop ledger-service
LEDGER_FAULT_SKIP_APPEND=true docker compose up -d ledger-service
```

Log in as ops, do a transfer as the customer (UI Transfer tab), then
reconcile the source account.

The agent reports **MISMATCH** with:
- the delta (`balance` vs `ledgerSum`),
- the root cause (`direction`: `MISSING_DEBIT_LEG` / `MISSING_CREDIT_LEG`,
  the amount, and the anchor `balanceAfter`/`entryId`),
- the **evidence trail** (last 12 of N ledger entries: entry id, type, signed
  amount, payment ref, running balance),
- a **proposed corrective journal entry** (Dr/Cr pair marked NOT executed —
  requires ops review; real corrective writes are explicitly out of scope).

Bonus guardrail beat: ask the ops console to `transfer 50 to savings` →
denied ("ops powers are investigation-only"). Ops can investigate but never
move customer money.

## 6. Guardrail: internal reads are EMPLOYEE-only

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"demo@bank.dev","password":"demo1234"}' | jq -r .data.token)
curl -si http://localhost:8080/api/ledger/internal/acc-savings-0002 \
  -H "Authorization: Bearer $TOKEN" | head -1
# → HTTP/1.1 403  (EMPLOYEE role required)
```

A USER calling `/api/accounts/internal/balance/acc-savings-0002` gets 403 too,
and the customer-facing `/api/ledger/{id}` is ownership-scoped (other
accounts → 404).

## Open questions (resolved for the demo)

- **Live approve or narrative?** Live. Transfers via the agent produce
  `pendingSteps`; the UI shows the confirmation modal and re-calls with the
  same plan + idempotency key (executed exactly once).
- **LLM provider on demo day?** Gemini is the default primary
  (`AGENT_PROVIDER=gemini`): set `AGENT_GEMINI_API_KEY` in
  `infra/.env` → live Gemini planning via Spring AI's `google-genai` starter
  (gemini-2.5-flash). Alternative:
  `AGENT_PROVIDER=ollama` + `SPRING_AI_OLLAMA_BASE_URL` (a real Spring AI
  property) → local Ollama (llama3.1:8b). No key set → deterministic
  keyword planner (presented knowingly as the deterministic upgrade path).

## Reset

```bash
docker compose down -v   # drops volumes; re-seeds on next up
```