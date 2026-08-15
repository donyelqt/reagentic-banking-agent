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
- SPA → Cloudflare Pages (`vite build`); expose the gateway via Cloudflare
  Tunnel. Set `VITE_GATEWAY_URL` to the public gateway URL.
- Terraform (Azure) is left as an ADR decision (no `terraform apply` in MVP).

## Required environment variables
`JWT_SECRET`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`,
`*_DB_URL`, `KAFKA_BOOTSTRAP_SERVERS`, `*_SERVICE_URL`, `VITE_GATEWAY_URL`,
optional `SPRING_AI_OLLAMA_BASE_URL`. See `infra/.env.example`.
