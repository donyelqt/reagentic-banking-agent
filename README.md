# Reagentic Banking Agent

A small but **real** online-banking system (our demo "Reagentic Bank") built as
Java/Spring Boot microservices, with an **AI assistant wired into the real
backend**. Balances are numbers in PostgreSQL — no real money.

> Demo user: `demo@bank.dev` / `demo1234` · Checking `acc-checking-0001` ($1000.00), Savings `acc-savings-0002` ($500.00).

## Stack
- **Backend + agent:** Java 21 / Spring Boot 3.3 (Maven multi-module monorepo)
- **Frontend:** React + Vite + TypeScript + Tailwind
- **Broker:** Kafka (immutable audit log)
- **Auth:** hand-rolled JWT (Spring Security + JJWT), zero internal trust
- **Money:** `BigDecimal` scale 2, JSON **strings** on the wire (`"1000.00"`)

## Modules
| Module | Port | Responsibility |
|---|---|---|
| `gateway` | 8080 | External entry: verify JWT, CORS, route to services |
| `auth-service` | 8081 | JWT issue/verify; seed demo user |
| `account-service` | 8082 | Accounts + authoritative balances |
| `payment-service` | 8083 | Transfer saga + outbox |
| `ledger-service` | 8084 | Kafka consumer; append-only log |
| `notification-service` | 8085 | Kafka consumer; confirmation stub |
| `ai-agent` | 8086 | Multi-step agent over the backend tools |
| `frontend` | 5173 | Login, Dashboard, Transfer, Agent chat |

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

## The hero use case
`reconcileAccount` enforces `balance == Σ(signed ledger entries)`.
1. Clean account → agent reports **"balanced."**
2. Inject a break: set `LEDGER_FAULT_SKIP_APPEND=true`, restart `ledger-service`,
   do a transfer (it is consumed/acked but **not** appended), then reconcile →
   agent **explains the missing leg** with evidence.
3. Agent transfers require explicit approval (`pendingSteps`); the frontend
   prompts, approval re-calls with the same idempotency key (executed exactly once).

## Docs
- [`docs/architecture.md`](docs/architecture.md)
- [`docs/deployment.md`](docs/deployment.md)
- [`docs/demo-runbook.md`](docs/demo-runbook.md)
