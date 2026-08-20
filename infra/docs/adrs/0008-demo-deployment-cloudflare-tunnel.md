# ADR-0008: Demo deployment via Cloudflare Tunnel (no active cloud billing account)

## Status
Accepted

## Date
2026-08-20

## Owner
Doniele (decision-maker & infrastructure owner)

## Context
The Cloud Elite internship demo day requires the platform to be reachable at a
public URL — not just "works on my machine." Our stack is fully built and
CI-green (see ADR-0006) but only ever runs via local `docker compose`
(see ADR-0002) — there is no public endpoint today.

The blocker is a constraint, not a scope choice:

- **GCP is unavailable.** Cloud Run requires an active GCP billing account
  (required even on the free tier), and none exists — there is no billing
  account, and the previous one has expired. The `infra/terraform/azure`
  folder is empty for the same reason — no Azure subscription.
- **Azure for Students** ($100 credit, no credit card) was attempted and
  **rejected — the UC email is not accepted** by the program.
- The stack is deliberately cloud-agnostic: every service reads env vars
  (`*.DB_URL`, `*_SERVICE_URL`), so the same topology can run anywhere with
  zero code changes.

Requirement: a public HTTPS URL serving the full 7-service stack + Kafka +
Postgres + frontend, with zero recurring cost, before demo day.

## Decision
Run the exact same `docker compose` topology on a host we control and expose it
through a **Cloudflare Tunnel** — a named tunnel + owned domain for a stable
judge-facing URL (quick `trycloudflare.com` tunnels get random hostnames per
restart, so they are only good for throwaway tests).

- The gateway (`:8080`) and frontend become public; internal services stay on
  the compose network, unreachable from the internet.
- No Java code changes, no container rewrite, no provider migration — the demo
  runs the same verified stack that CI tests. Not turnkey, though: verified
  against the running stack, the following config is required:
  - `VITE_GATEWAY_URL` is baked at frontend build / dev-server start
    (`frontend/src/api.ts`; `frontend/.env` pins `http://localhost:8080`). A
    remote browser on a public URL would resolve `localhost:8080` against its
    *own machine* and every API call would die. The frontend must be built or
    served with `VITE_GATEWAY_URL` pointing at the public gateway.
  - The gateway's default CORS allows `http://localhost:*` only
    (`GATEWAY_CORS_ORIGINS`). A two-tunnel setup (frontend and gateway on
    different public origins) is CORS-blocked unless the gateway is configured
    with the tunnel origin and restarted.
  - The frontend is **not containerized** (`infra/docker-compose.yml`); there
    is no frontend image to tunnel. Workable shapes are: (a) a single tunnel
    to the dev server on `:5173` (its Vite proxy already forwards `/api` to
    the gateway), or (b) a `vite build` to Cloudflare Pages with
    `VITE_GATEWAY_URL` set, plus a tunnel for the gateway.

Status: **planned — NOT implemented.** This ADR records the chosen route and
the constraint; the tunnel was **not executed** for the 2026-08-20 submission
(no public URL exists). Revisit only if a later demo needs a public URL.
`docs/deployment.md` tracks live status.

## Architecture & Infrastructure Judgement
- **Why I stand behind this decision.** The constraint is immutable for the
  demo window (no billing account — none exists and the prior one expired;
  rejected student email). Within that
  constraint, the tunnel maximizes leverage: it converts an already-verified
  local topology into a live URL in minutes, and it exercises the same
  artifacts the judges care about (auth, Kafka, ledger, agent) rather than a
  downgraded stand-in.
- **What this decision accepts, consciously.** A host-side dependency — the
  URL lives while the host does; demo-day uptime is a personal responsibility,
  not a platform SLA. The tunnel does not replace the real cloud path; it
  closes the demo-day gap *and* is disposable if a billing path appears later.
- **Boundary of the judgement.** This is a demo-delivery decision, not the
  production target. ADR-0006 already records the intended production shape
  (Azure ACI staging, AKS skeleton, OIDC credentials). If a billing path
  becomes available, the migration target is that shape — the tunnel is
  torn down, not adopted.

## Alternatives Considered

### GCP Cloud Run
- Pros: Managed, serverless, scales to zero.
- Cons: **Requires an active billing account even on free tier** — none
  exists (no billing account; the prior one expired). This is a hard blocker,
  not a tradeoff.
- Rejected: Infeasible within the demo window.

### Azure for Students / Azure ACI
- Pros: $100 credit with no credit card; the ADR-0006 production target.
- Cons: **The UC email is rejected by the Azure for Students program.** No
  other eligible email is available.
- Rejected: Unavailable in practice.

### Render free tier
- Pros: Free web services and Postgres, no credit card, clean dashboard.
- Cons (verified against Render's docs): native runtimes are Node/Bun, Python,
  Ruby, Go, Rust, Elixir — **no JVM**, so Spring Boot requires Docker deploys;
  no managed Kafka (our KRaft Kafka would be a hand-rolled container); free
  instances are 512 MB / 0.1 CPU, spin down after 15 min idle (kills a live
  demo), have an ephemeral filesystem, cannot receive private network traffic,
  and the 750 free instance-hours/month per workspace cannot cover 8 always-on
  services (8 × 720 h/mo needed) — they would be suspended within days. Free
  Postgres expires after 30 days (1 GB fixed, one per account).
- Rejected: Unsuitable for a 7-service + Kafka + Postgres topology on the free
  tier; the paid path costs more than the constraint allows.

### Paid VM (Azure/AWS/GCP small instance)
- Pros: Full control, can run the compose stack verbatim.
- Cons: Requires a payment method / credits — the same constraint that blocks
  GCP and Azure for Students.
- Rejected: No billing path exists to pay for it.

## Consequences
- A public HTTPS URL for the full stack before demo day, zero recurring cost,
  no Java code changes — a live deployment of the verified stack.
- Demo-day uptime depends on the tunnel host; mitigated by keeping the
  compose stack fully reproducible and the tunnel command in the runbook.
- The tunnel exposes only the gateway and frontend; internal services remain
  on the compose network (no direct internet exposure).
- If a billing path appears later, migrate to the ADR-0006 target (Azure
  ACI/AKS with OIDC) and tear the tunnel down; this ADR is superseded then,
  not before.

## References
- `docs/deployment.md` — "Live demo URL plan" (operational status tracker)
- ADR-0006 — CI/CD + intended Azure production target
- ADR-0002 — docker-compose local topology (referenced, file pending)