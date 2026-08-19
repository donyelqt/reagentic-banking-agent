# Reagentic Banking Agent — Product Direction

> **Status:** Living document — single source of truth for product scope and direction.
> **Owner:** Doniele (internship deliverable, Accenture Cloud Elite) with team contributions.
> **Supersedes:** the product framing in `docs/ideas/` and any competing pitch framing; see §7 for the decision log.

---

## 1. Executive Summary

We're building an **agentic banking operations platform** — a real multi-service online bank whose AI assistant does not just answer questions, it *proposes and executes bank operations* behind an enforced human-approval gate. For a customer, the agent checks balances, reads transactions, and arranges transfers that never execute without explicit confirmation. For an ops analyst, it reconciles accounts against the immutable ledger, finds the root cause of a break, and proposes the corrective journal entry for review. The result is a demo that proves an AI can be trusted around money — because the system's guardrails are backend invariants, not model suggestions.

The product spine is **Direction B** (agentic operations). The insight arc (spending visualization + "analyze my spending") is an approved feature on top of that spine — adopted from the team pitch (§8). CSV is a two-way decision: upload as a core mechanism is **rejected**, while an RFC 4180-clean statement **export** is **approved** — the bank's records leave as data, but no external file ever becomes a source of truth (§7). The customer-facing shell (landing funnel, chat onboarding with clickable capability chips) is shipped and demo-first (§10).

## 2. Positioning Statement

### Value Proposition

**For** banking customers and ops analysts who need the bank's records explained and acted on
**that need** answers with *proof and control* — not just chat
**Reagentic Bank**
**is a** banking platform with an embedded agentic operations assistant
**that** finds and proves what's wrong (reconciliation with root cause), moves money only after explicit human approval, and keeps every step on an immutable audit trail.

### Differentiation Statement

**Unlike** consumer finance apps that show static charts and advice (and unlike chatbot demos that only talk)
**Reagentic** **provides** an AI that *operates* the bank: it executes real transfers behind a server-enforced approval gate, reconciles the real ledger with an evidence trail, and enforces two mutually-exclusive roles — while every action stays idempotent and replayable.

### One-line hook

> "An AI that can move money — but only when a human says so, and it can prove every step."

## 3. Problem Statement

- **Who has this problem?** Banks whose ops teams reconcile ledgers by hand; customers who can't understand why their records don't match.
- **What is the problem?** When a ledger breaks, finding the missing entry is a slow manual hunt across millions of rows. At the same time, banks rightly refuse to let an AI touch money without proof and controls — so most "AI banking" demos stop at chat and charts.
- **Why is it painful?** Manual reconciliation costs banks thousands of man-hours per year; exceptions stay outside straight-through processing (STP). For the intern demo, the pain is credibility: an AI that *talks* about money is a gimmick; an AI that *acts* with guardrails is engineering.
- **Evidence:** the reconciliation invariant `balance == Σ(signed ledger entries)` is enforced in code (`reconcileAccount`, see `docs/architecture.md`); the approval gate is a backend invariant, not a model suggestion (PR #3 hardening); the two-role model is enforced at the service layer (internal endpoints are EMPLOYEE-only).

## 4. Target Users & Personas

### Primary persona: Customer "Dani"
- **Role:** Account holder at Reagentic Bank (demo user `demo@bank.dev`).
- **Goals:** Know balances/transactions instantly; move money safely; understand spending.
- **Pain points:** Call centers are slow; banking apps show history, not reasons; fears AI touching money.
- **Behavior:** Asks the agent in plain language; expects a confirmation step before anything moves.

### Secondary persona: Ops analyst "Omar"
- **Role:** Employee-side analyst (`ops@bank.dev`, EMPLOYEE role).
- **Goals:** Investigate any account; find and explain breaks; never move customer money.
- **Pain points:** Reconciliation is manual and slow; evidence trails live in different systems.
- **Behavior:** Uses the Reconciliation Console; expects root-cause proof, not just "it's broken".

## 5. Strategic Context

- **Business goal:** Deliver an unmistakably enterprise-grade banking demo for the Accenture Cloud Elite internship — one that graders recognize as real engineering, not a tutorial.
- **Why now:** The platform (6 services, saga/outbox, Kafka ledger, JWT, dual-role agent) is built, tested (93/93), and demo-ready. The remaining work is the insight arc that makes the demo's opening act visual.
- **Competitive landscape (peers):** Most cohort demos are a single Spring Boot service plus a chatbot, or read-only analytics dashboards. Both are beaten by a system where the AI *acts* with proof and guardrails.
- **Decision this doc records:** Direction B is the product. See §7 for the full decision log.

## 6. Solution Overview

A multi-service bank (gateway → auth/account/payment/ledger/notification) with an AI agent wired into the real backend via typed tool calls, JWT propagation (zero internal trust), and four pillars:

### Pillar 1 — Agentic operations with enforced guardrails
- The agent plans (LLM primary, deterministic keyword fallback), executes only approved steps, and transfers carry server-enforced `confirmationRequired=true` plus idempotency keys (exactly-once).
- **The money moves only after a human approves — enforced in code, not requested from the model.**

### Pillar 2 — Dual-role security model
- USER: ownership-scoped reads + supervised transfers. EMPLOYEE: ops console, reconciliation over any account, internal reads — and no transfer powers.
- Internal endpoints are EMPLOYEE-only at the service layer; a customer token can never reach them.

### Pillar 3 — Event-driven money
- Transfers run a saga via transaction outbox → Kafka; the ledger is append-only with `balance_after` on every entry; notification follows events.
- The money state is authoritative in account-service; the ledger is the immutable audit trail.

### Pillar 4 — Grounded AI
- Plan → execute → evidence. LLM outputs are parsed into a validated plan DAG; unknown tools or malformed transfers fall back to the deterministic planner.
- Classification (category + 1:1 contract) is verified per item; drifted responses fall back per item — money categories are never silently mislabeled.

## 7. The Decision Log

| Decision | Choice | Rationale |
|---|---|---|
| Product spine | **Direction B: agentic banking operations** | The platform is built, tested, and defensible; it is the harder and more impressive engineering |
| Insight arc | **Adopted as a feature** | "Understand my money" is a strong demo opening act; it runs on the bank's own ledger data |
| CSV upload as core mechanism | **Rejected** | We are the bank, not accounting software. The ledger is authoritative; an imported file creates a second truth the ledger must reconcile against — and the industry's documented CSV-import failure mode *is* duplicates and re-import drift (Continia reimport prevention, NetSuite/Cobase refresh duplicates, Sage "import ran twice, two copies of the same debit"). Two sources of truth is the disease this architecture exists to prevent. Rich history comes from a seed migration, not user uploads |
| CSV statement export | **Adopted as a feature** | Read-only, RFC 4180-clean snapshot of the ledger (headers, UTF-8, CRLF, ISO dates, signed amounts, running balance) — the portability every real bank ships (Westpac, Huntington, PPF Banka, ECB T2S, Canada Open Finance). Zero drift risk: the ledger stays authoritative; the export is a projection. Bonus: real-bank CSVs are famously broken (StatementPro), so "export done right" is a demo talking point. A styled XLSX workbook (POI) is the human view of the same projection — branded navy title bar, gold header, banded rows, `$#,##0.00` amounts |
| Agent onboarding (capability pre-prompt + clickable chips) | **Adopted** | "What can you do?" is the discoverability hook: a pinned prompt that opens the capability map, then role-aware action chips that make the demo self-explaining to judges without a help menu |
| Live bank / Open Banking integration | **Out of scope** | BSP accreditation is not attainable in-sprint; the ledger is the more credible data source anyway |
| Corrective journal execution | **Out of scope** | The agent proposes; humans execute. This is the trust story, not a limitation |

## 8. The Insight Arc (committed next)

Adopted from the team pitch, fed by the bank's own data — no CSV:

1. **`V3__seed_demo_history.sql`** — a year of realistic, categorized transactions whose `balance_after` chain lands exactly on the seeded balances (self-consistent by construction; reconciliation stays green). **Required, not nice-to-have:** the current cash-flow chart exposes the sparse-history gap — the axis reads "Jan 1, Aug 16-18" because seed data has a seven-month dead zone; charts are only believable with real history.
2. **Dashboard charts** — spending by category from `/api/agent/classify` over real ledger transactions (one source of truth). Cash-flow chart (recharts) is live; the category breakdown (the mislabeled balance pie) and the analyze action are the remaining work.
3. **"Analyze my spending" chat action** — the agent summarizes where money went and flags patterns (e.g., overspending on dining), then can *act* on it (transfer + approval modal).

**Demo narrative (rising arc):** insight ("you overspent on dining") → propose ("transfer 500 to savings?") → approve (modal) → the ledger moves on screen.

## 9. Success Metrics

| Metric | Target |
|---|---|
| Demo-time: hero flows execute live without failure | reconcile (clean + injected break), supervised transfer, 403 role denials, self-explaining chat onboarding ("What can you do?" → chips → approve) |
| Test suite | 93/93 passing (`mvnw test`), no regressions on PR merge |
| Guardrail verification | transfer without approval is impossible (server-enforced); LLM drift falls back, never mislabels |
| Insight arc demo beat | charts + analyze action live from ledger data within the next increment |
| CSV export | CSV (RFC 4180) and styled XLSX, both verifiable against the ledger (row count == Σ entries; balance column == `balance_after`); USER + EMPLOYEE routes |

## 10. User Stories & Requirements

**Story 1 — Supervised transfer (USER)**
As a customer, I want the agent to arrange a transfer but require my confirmation, so I control every movement.
   - [x] Agent returns `pendingSteps`; nothing executes
   - [x] Frontend shows the plan (from/to/amount) and an Approve action
   - [x] Approval re-calls with the same idempotency key; executing twice yields one payment
   - [x] A plan step for `transferFunds` can never arrive with `confirmationRequired=false` (trust-boundary invariant in `Executor`; planner flag + executor enforcement; regression test in `ExecutorTest`)

**Story 2 — Reconciliation with root cause (EMPLOYEE)**
As an ops analyst, I want to reconcile any account and see the mismatch's cause, so I can fix breaks fast.
- [x] Clean account reports BALANCED
- [x] Injected fault reports the delta, direction (MISSING_DEBIT_LEG / MISSING_CREDIT_LEG), anchor entry, and an evidence trail
- [x] A corrective journal entry is proposed, never executed
- [x] Ops cannot transfer funds (role denial)

**Story 3 — Spending analysis (USER, insight arc)**
As a customer, I want to see where my money went, so I can act on wasteful patterns.
- [x] Dashboard shows spending by category from the ledger
- [x] "Analyze my spending" returns a categorized summary with totals
- [x] Insight can lead directly into a proposed (approved) action
  - Status: complete. Backend `/api/agent/classify` (categorized summary, per-item fallback, edge cases) + V3 demo-history seed (12-month net-zero chain, Jul 2025–Jul 2026, descriptions) + frontend wiring: `classifySpending` batch helper (≤100/request, spending-only, merged summary), CategoryChart now a real spending-by-category pie (skeleton/empty/error+retry states), "Analyze my spending" chat action in both AgentChat and FloatingChat (deterministic, no LLM key needed). Verified end-to-end on a fresh-volume stack: 301 entries, reconcile BALANCED (1000.00 = 1000.00), 9 live categories (UTILITIES largest).

**Edge cases covered:** garbage classify input → 400 with precise message; >100 items → 400; null elements → 400; LLM reorders classifications → per-item fallback; model unreachable → deterministic fallback; unknown tool in plan → whole plan falls back.

## 11. Out of Scope

- **CSV upload as a core mechanism** — rejected (§7); the bank analyzes its own records. CSV *export* is in scope (§7)
- **Live bank / Open Banking / BSP-regulated integrations**
- **Executing corrective journal entries** — propose-only
- **Fraud detection / anomaly scoring** — later vision (see §13)
- **Multi-bank aggregation, mobile apps, real notifications (SMS/push), CSV *import* of any kind**

## 12. Dependencies & Risks

| Risk | Mitigation |
|---|---|
| Demo day LLM key absent | Deterministic fallback is the default; Gemini key is a one-line `.env` change (see `docs/demo-runbook.md`) |
| Insight arc slips scope | It is the *only* committed next increment; everything else stays proposed |
| Sparse seed history makes charts unbelievable (Jan-1-to-Aug gap on the axis) | `V3__seed_demo_history.sql` lands before chart wiring ships; the balance chain is self-consistent by construction |
| CSV export scope creep (parsers, formats, pagination) | Export is a read-only projection: two formats (RFC 4180 CSV + styled XLSX) from the same ledger rows, no import counterpart, no parsing |
| Stale jar in the demo image | Build the jar with `mvnw package` before `docker compose up --build` (documented in the demo runbook) |
| Team story divergence (two pitch versions) | This doc is the canonical direction; the deck should be updated from it |

## 13. Roadmap

- **Done:** platform, dual-role agent, guardrails, classification, 93/93 tests, PR #3 hardening, landing page funnel (PR #4), chat onboarding — "What can you do?" capability prompt + clickable action/follow-up chips (PR #5), CSV-free landing narrative (PR #16), statement export — RFC 4180 CSV + styled XLSX (POI), USER + EMPLOYEE routes, humanized descriptions, floating chat redesign, Story 3 insight arc (spending-by-category chart + "Analyze my spending" action + 12-month seeded history)
- **Next (committed):** insight arc — V3 seed history → category charts + analyze action
- **Later (vision only, pitch-ready):** fraud detection, anomaly scoring, more agent tools, CI/CD test gate on merge (ADR 0006) + branch protection

## 14. Open Questions

- ~~Charts library choice~~ — **Resolved:** recharts, already in production use for the cash-flow chart. Remaining: the chart data contract with `/classify` (response shape for category breakdown)
- ~~CSV export placement and scope~~ — **Resolved:** ledger-service serves `GET /api/ledger/{accountId}/statement.csv` (ownership-scoped via account-service, 404 for cross-account) and `GET /api/ledger/internal/{accountId}/statement.csv` (EMPLOYEE-only, any account), mirroring the existing list endpoints; RFC 4180-clean (UTF-8 BOM, CRLF, ISO-8601 UTC dates, signed amounts, running balance)
- Whether the seed history lives in ledger-service migration or account-service (ledger is the source for chart data)
- Where the CSV export endpoint lives (ledger-service owns the projection; account-service owns entitlements) and whether it's scoped to USER or also EMPLOYEE
- Demo-day LLM provider: Gemini key set, or deterministic fallback presented knowingly
