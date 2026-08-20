# Reagentic Banking Agent

A small but **real** online-banking system (our demo "Reagentic Bank") built as
Java/Spring Boot microservices, with an **AI assistant wired into the real
backend**. Balances are numbers in PostgreSQL — no real money.

> **Positioning:** an AI that can move money — but only when a human says so, and it can prove
> every step. Product direction, scope decisions, and roadmap live in
> [`docs/PRODUCT_DIRECTION.md`](docs/PRODUCT_DIRECTION.md).

> Demo users: customer `demo@bank.dev` / `demo1234` (USER) · ops analyst `ops@bank.dev` / `ops1234` (EMPLOYEE). Accounts: Checking `acc-checking-0001` ($1000.00), Savings `acc-savings-0002` ($500.00).

> **Source of truth:** this repository — **backend and frontend** — is the
> authoritative artifact. External documents (e.g., the submitted Google Doc)
> may be **outdated** — if they conflict with this README or `docs/`, the
> repository wins. Verify any claim with the
> [Verify commands](#verify-what-ci-runs) rather than a copy.

> **No mock data:** the frontend hardcodes nothing — every balance,
> transaction, ledger row, and reconcile result is **fetched live** from the
> backend (Postgres + Kafka) over authenticated APIs. The dataset is
> **demo-seeded** in Postgres (`DataSeeder`), not hardcoded in the UI.

## Stack
- **Backend + agent:** Java 17 / Spring Boot 3.5.16 (Maven multi-module monorepo, JDK 17 pinned in CI)
- **Frontend:** React + Vite + TypeScript + Tailwind
- **Broker:** Kafka (immutable audit log)
- **Auth:** JWT (Spring Security + JJWT), **zero internal trust** — every
  service independently validates the caller JWT; no internal call is trusted
  by default
- **Money:** `BigDecimal` scale 2, JSON **strings** on the wire (`"1000.00"`)

## Security & engineering posture

Evidence-backed posture — what this repo actually does, and what it
deliberately does not (yet).

**Security (zero internal trust):**
- Every service with HTTP endpoints ships its own `SecurityConfig` + `JwtFilter`
  (auth, account, payment, ledger, ai-agent) — the gateway is the entry point,
  not the trust boundary; a forged or missing token fails at the service it
  hits. Inter-service mutates (`debit`/`credit`) authenticate with a short-lived
  signed service JWT (subject-bound, role `SERVICE`); the gateway strips the
  legacy internal headers (`X-Service-Token`, `X-User-Subject`) from all
  external requests.
- Reads are **ownership-scoped at the service layer** (`loadOwned`), not by
  convention: a USER token can never see another customer's accounts.
- Role gates are enforced at the service layer, not the UI: EMPLOYEE-only
  internal endpoints (`/api/accounts/internal/**`, `/api/ledger/internal/**`)
  reject USER tokens with 403; EMPLOYEE tokens cannot move money (transfers
  are denied for ops).
- Money movement is **approval-gated and idempotent**: mutating agent steps
  become `pendingSteps` requiring explicit approval, execution re-calls with the
  same idempotency key — executed exactly once. Approval is enforced **at the
  API boundary**: the ai-agent mints a short-lived signed `TRANSFER`
  authorization only for an approved `transferFunds` step, and `payment-service`
  rejects any transfer that does not present it (403) — a direct call to
  `/api/payments/transfer` cannot skip the approval flow.
- Money is `BigDecimal` scale 2, serialized as JSON **strings** — no float
  drift, no wire-format ambiguity.
- Secrets live in environment variables only (`infra/.env.example` is the
  template); no credentials, keys, or tokens in the repository or CI.
- Frontend persists the auth token in `localStorage` for session continuity
  across reloads — a known XSS trade-off, tracked under gaps below.

**Engineering (CI-verified):**
- 115 unit tests across the Maven reactor, gated by GitHub Actions CI on every
  PR/push to `main`/`donieledev` (compile, tests, frontend typecheck + build,
  production dependency audit) — see ADR-0006.
- Decisions are recorded, not recalled: ADRs in `infra/docs/adrs/` document
  topology (0001), CI/CD (0006), the agent harness (0007), and the deployment
  plan (0008).

**Known gaps (tracked, not hidden):**
- Rate limiting (auth and API endpoints) — not yet implemented.
- Frontend unit tests — not yet present (typecheck + build are gated).
- Auth token persists in `localStorage` for session continuity — an accepted
  XSS trade-off for the demo; a hardened build would move to short-lived
  memory/session tokens.
- Live deployment — planned but not executed (no cloud billing account; see
  ADR-0008 and `docs/deployment.md`).

## Modules
| Module | Port | Responsibility |
|---|---|---|
| `gateway` | 8080 | External entry: verify JWT, CORS, route to services |
| `auth-service` | 8081 | JWT issue/verify; seed demo user |
| `account-service` | 8082 | Accounts + authoritative balances |
| `payment-service` | 8083 | Transfer saga + outbox |
| `ledger-service` | 8084 | Kafka consumer; append-only log |
| `notification-service` | 8085 | Kafka consumer; confirmation stub |
| `ai-agent` | 8086 | Agent for both roles: USER (personal agent — balances, transactions, supervised transfers) and EMPLOYEE (ops console — reconcile with root-cause evidence, internal reads) |
| `frontend` | 5173 | Landing, Login, Dashboard (spending insights + activity feed), Transfer (USER) / Ledger Console + reconcile evidence (EMPLOYEE), agent chat for both |

`common/` holds shared `Money`, `JwtUtil`, events, and `DemoConstants`.

## Run with Docker (one command)
```bash
cd infra
cp .env.example .env        # set a >=32-byte JWT_SECRET
docker compose up --build
# frontend (host):  cd frontend && npm install && npm run dev  (http://localhost:5173)
```
For <=16 GB hosts use the reduced-heap overlay:
```bash
docker compose -f docker-compose.yml -f docker-compose.demo.yml up --build
```

## Local (bare) dev
Each service reads env vars (`JWT_SECRET`, `*_DB_URL`, `KAFKA_BOOTSTRAP_SERVERS`,
`*_SERVICE_URL`). See `infra/.env.example`. `mvnw compile` builds everything.

## Verify (what CI runs)
```bash
.\mvnw -B test                                    # backend: 115 tests, 8 modules (JDK 17)
cd frontend && npm ci && npm run typecheck && npm run build   # frontend gates
```
The same gates run in GitHub Actions on every PR/push (see ADR-0006).

## Two-role access model

One system, two roles (see `docs/ideas/two-role-agent-access-model.md`):

- **USER** keeps the ownership-scoped customer path (accounts, balances,
  supervised transfers) **plus a personal agent**: balances, transactions,
  and transfers with an explicit approval gate. Cross-account tools
  (`reconcileAccount`, internal reads) are denied with a clear message.
- **EMPLOYEE** gets the ops console: `reconcileAccount` over any account with
  root-cause evidence, and the internal reads that power it
  (`/api/accounts/internal/**`, `/api/ledger/internal/**`) are EMPLOYEE-only at
  the service layer — a customer token can never reach them. Ops cannot move
  customer money (transfers are denied for EMPLOYEE).

## The hero use case

`reconcileAccount` enforces `balance == Σ(signed ledger entries)`.
1. Clean account → agent reports **"balanced."**
2. Inject a break: `LEDGER_FAULT_SKIP_APPEND=true` + restart `ledger-service`,
   do a transfer (consumed/acked but **not** appended), then reconcile →
   agent reports the **mismatch with root cause** (`MISSING_DEBIT_LEG` /
   `MISSING_CREDIT_LEG`), a **12-entry evidence trail**, and a **proposed
   corrective journal entry** (not executed — ops review required).
3. Agent transfers require explicit approval (`pendingSteps`); the frontend
   prompts, approval re-calls with the same idempotency key (executed exactly
   once).

## Docs
- [`docs/PRODUCT_DIRECTION.md`](docs/PRODUCT_DIRECTION.md) — product direction, decisions, roadmap (canonical)
- [`docs/architecture.md`](docs/architecture.md)
- [`docs/deployment.md`](docs/deployment.md)
- [`docs/demo-runbook.md`](docs/demo-runbook.md)
- [`infra/docs/adrs/`](infra/docs/adrs/) — architecture decision records (ADR-0001 repo topology, ADR-0006 CI/CD, ADR-0007 Spring AI + custom agent harness, ADR-0008 demo deployment via Cloudflare Tunnel)
