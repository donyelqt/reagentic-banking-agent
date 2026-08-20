# Deployment

## Local (Docker Compose) — no credit card
```bash
cd infra
cp .env.example .env          # set JWT_SECRET (>=32 bytes) and DB credentials
docker compose up --build
```
- Single Postgres instance with 5 databases (`auth`, `account`, `payment`,
  `ledger`, `notification`) created by `infra/postgres/init.sql`.
- Kafka runs in KRaft mode (no Zookeeper). Topic `payment-events` is
  auto-created.
- Each service is built from source via the shared `infra/Dockerfile`
  (`MODULE` build arg) — one image recipe for all 7 services.
- The frontend is **not** containerized; run it on the host
  (`npm install && npm run dev`, port 5173) or build to Cloudflare Pages.

### Low-RAM hosts (<=16 GB)
```bash
docker compose -f docker-compose.yml -f docker-compose.demo.yml up --build
```
Applies reduced JVM heap (`-Xmx256m`, agent `-Xmx384m`).

## Cloud-ready (optional)
- Postgres → Aiven / Neon via `*.DB_URL` env (no code change).
- SPA → Cloudflare Pages (`vite build` with `VITE_GATEWAY_URL` set); expose the gateway via Cloudflare
  Tunnel. See the ADR-0008 Decision for the required config (CORS, frontend serving, stable URL).
- Terraform (Azure) is left as an ADR decision (no `terraform apply` in MVP);
  the deferral is recorded in ADR-0008.

## Live demo URL plan (status: planned — NOT implemented)

Goal: the full stack reachable at a public HTTPS URL before demo day. The
stack is cloud-agnostic — every path below runs the **same** `docker compose`
topology with zero code changes. Decision and rationale: see
[ADR-0008](../infra/docs/adrs/0008-demo-deployment-cloudflare-tunnel.md).

**Status as of submission (2026-08-20): this plan was NOT executed.** The
stack runs locally via `docker compose` (verified working); no public URL
exists. The tunnel was never set up.

- **Constraint:** no active GCP billing account (none exists — and the
  previous one has expired), so Cloud Run is not available
  (GCP requires an active billing account for Cloud Run even on free tier).
  This is why no cloud deployment exists yet — a billing constraint, not a
  scope decision.
- **Path 1 — Cloudflare Tunnel (fastest, $0, no account or card):** run
  `docker compose` from any machine, then a tunnel exposes the app publicly.
  Not turnkey: `VITE_GATEWAY_URL` must point at the public gateway
  (baked at build/dev-server start), gateway CORS (`GATEWAY_CORS_ORIGINS`)
  must allow the tunnel origin, the frontend must be reachable (single tunnel
  to the dev server on `:5173`, or a Pages build), and a **named tunnel +
  owned domain** is required for a stable judge-facing URL — quick-tunnel
  hostnames are random per restart. See the ADR-0008 Decision for the full
  configuration.
- **Path 2 — Azure for Students ($100 credit, no credit card):** **attempted
  and rejected** — the UC email is not accepted by the Azure for Students
  program. Not viable unless another eligible email becomes available.
- **Path 3 — Render free tier: analyzed and rejected as unsuitable.** Render's
  native runtimes are Node/Bun, Python, Ruby, Go, Rust, Elixir — **no JVM**, so
  our Spring Boot services would need Docker deploys (free tier has no managed
  Kafka, and our KRaft Kafka would be a hand-rolled container). Free instances
  are 512 MB / 0.1 CPU, **spin down after 15 min idle**, have an ephemeral
  filesystem, cannot receive private network traffic, and the 750 free
  instance-hours/month per workspace cannot cover 8 always-on services
  (8 × 720h/mo needed) — they would be suspended within days. Free Postgres
  expires after 30 days (1 GB fixed). Render free tier only fits a single
  lightweight demo service, not a 7-service + Kafka + Postgres topology.
- **Current status:** NOT implemented as of submission — see the status note
  at the top of this section. The plan remains valid for a later demo if the
  stack needs a public URL.

## Required environment variables
`JWT_SECRET`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`,
`*_DB_URL`, `KAFKA_BOOTSTRAP_SERVERS`, `*_SERVICE_URL`, `VITE_GATEWAY_URL`,
optional `SPRING_AI_OLLAMA_BASE_URL`. See `infra/.env.example`.
