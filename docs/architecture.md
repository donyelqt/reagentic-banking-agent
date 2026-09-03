# Architecture

## Topology

```mermaid
graph TD
    Browser["Browser<br/>frontend :5173"] --> GW["gateway :8080<br/>JWT verify · CORS · Route"]

    GW -->|"/api/auth/**"| Auth["auth-service :8081"]
    GW -->|"/api/accounts/**"| Account["account-service :8082"]
    GW -->|"/api/payments/**"| Payment["payment-service :8083"]
    GW -->|"/api/ledger/**"| Ledger["ledger-service :8084"]
    GW -->|"/api/notifications/**"| Notify["notification-service :8085"]
    GW -->|"/api/agent/**"| Agent["ai-agent :8086"]

    Agent -.->|"service-to-service<br/>*_SERVICE_URL<br/>Authorization: Bearer <caller JWT>"| Account
    Agent -.->|same| Ledger
    Agent -.->|same| Payment

    subgraph Compose Network
        Auth
        Account
        Payment
        Ledger
        Notify
        Agent
    end

    subgraph Infra
        Kafka[("Kafka :9092<br/>KRaft<br/>topic: payment-events")]
        Postgres[("Postgres :5432")]
    end

    Payment -->|outbox relay| Kafka
    Ledger -.->|consume| Kafka
    Notify -.->|consume| Kafka

    Account -.-> Postgres
    Auth -.-> Postgres
    Payment -.-> Postgres
    Ledger -.-> Postgres
```

The `ai-agent` calls the backend **directly** (service-to-service) via
`*_SERVICE_URL`, copying the caller''s `Authorization` header. The gateway only
fronts the external frontend→agent hop. Every service re-verifies the JWT
(**zero internal trust**).

```mermaid
graph LR
    subgraph External
        FE[Browser]
    end
    subgraph Gateway
        GW2[gateway<br/>verifies JWT<br/>strips X-Service-Token<br/>strips X-User-Subject]
    end
    subgraph Services["All services verify JWT independently"]
        A1[auth-service]
        A2[account-service<br/>loadOwned + ROLE_SERVICE]
        A3[payment-service<br/>mint SERVICE JWT]
        A4[ledger-service<br/>EMPLOYEE-only internal]
        A5[ai-agent<br/>Executor roleGate]
    end
    FE -->|Bearer user JWT| GW2
    GW2 -->|"Bearer user JWT (forwarded)"| A5
    A5 -->|"Bearer user JWT (copied)"| A2
    A5 -->|"Bearer user JWT (copied)"| A4
    A3 -->|"Bearer SERVICE JWT<br/>sub=user, role=SERVICE<br/>short-lived, JWT_SECRET"| A2

    style A3 fill:#f59e0b,stroke:#b45309,color:#fff,stroke-width:2px
    style GW2 fill:#2563eb,stroke:#1e40af,color:#fff,stroke-width:2px
```

`/api/agent/**` serves **both** roles with a per-role tool matrix enforced in the
executor (the caller''s role comes from the verified JWT, not the client):

| Tool | USER (customer) | EMPLOYEE (ops) |
|---|---|---|
| `listAccounts` | own accounts | own (none seeded) |
| `getBalance` | ownership-scoped endpoint | internal endpoint, any account |
| `listTransactions` | ownership-scoped endpoint | internal endpoint, any account |
| `transferFunds` | ✅ with approval (`pendingSteps`) | ❌ denied — investigation-only |
| `reconcileAccount` | ❌ denied — ops only | ✅ evidence + corrective entry |

```mermaid
graph TD
    Planner["Planner: LlmPlanner (gemini) → fallback KeywordPlanner"] --> Plan["Plan DAG<br/>Step[] + pendingSteps"]
    Plan --> Exec["Executor<br/>topoSort + roleGate + approvalGate"]
    Exec -->|USER| U1["getBalance/listTransactions<br/>/api/accounts/{id}/balance<br/>/api/ledger/{id}"]
    Exec -->|EMPLOYEE| E1["getBalance/listTransactions<br/>/api/accounts/internal/**<br/>/api/ledger/internal/**"]
    Exec -->|USER + approved| U2["transferFunds<br/>TRANSFER auth minted<br/>403 without it"]
    Exec -->|EMPLOYEE| E2["reconcileAccount<br/>evidence 12 + journal entry"]
    Exec -.->|denied| D1["USER → reconcileAccount<br/>EMPLOYEE → transferFunds"]

    style D1 fill:#dc2626,stroke:#991b1b,color:#fff,stroke-width:2px
    style U2 fill:#16a34a,stroke:#15803d,color:#fff,stroke-width:2px
    style E2 fill:#16a34a,stroke:#15803d,color:#fff,stroke-width:2px
```

The agent''s internal reads (`/api/accounts/internal/**`, `/api/ledger/internal/**`)
are EMPLOYEE-only at the service layer — a USER token can never reach a
cross-account read, even if the planner proposes it. The customer-facing
`/api/ledger/{accountId}` is ownership-scoped via a delegated check against
account-service (other accounts → 404).

Internal **mutates** (`debit`/`credit`) are the only endpoints that may run under
a user-context **service** principal: `payment-service` mints a short-lived
signed JWT (subject = the acting user, role = `SERVICE`, signed with the shared
`JWT_SECRET`) and presents it as the `Authorization` header, so `account-service`
still enforces ownership while granting `ROLE_SERVICE` for the leg. There is no
static shared bearer: the legacy `X-Service-Token`/`X-User-Subject` path is gone,
and the gateway strips those headers from every external request. Anything else
under `/api/accounts/internal/**` remains EMPLOYEE-only.

Transfers are approval-gated **at the API boundary**: the ai-agent mints a
short-lived signed `TRANSFER` authorization (bound to the caller''s subject and
the transfer''s idempotency key) only after an approved `transferFunds` step has
passed the executor''s approval gate, and `payment-service` rejects any transfer
that does not present it (`403 TRANSFER_UNAUTHORIZED`). A direct call to
`/api/payments/transfer` without that authorization is refused — approval is
server-enforced, not a UI convention.

## Data flow — a transfer

```mermaid
sequenceDiagram
    participant User as Browser
    participant Agent as ai-agent<br/>Executor
    participant Payment as payment-service
    participant Account as account-service<br/>Postgres
    participant Kafka as Kafka<br/>payment-events
    participant Ledger as ledger-service
    participant Notify as notification-service

    User->>Agent: POST /api/agent/chat<br/>transferFunds?<br/>pendingSteps returned
    Agent->>User: 200 {plan, pendingSteps, reply}
    User->>Agent: POST /api/agent/chat<br/>plan + approved stepIds<br/>TRANSFER auth minted
    Agent->>Payment: POST /api/payments/transfer<br/>Authorization: TRANSFER JWT<br/>idempotencyKey distinct per leg
    Payment->>Account: debit(source) SERVICE JWT<br/>sub=user, role=SERVICE
    alt debit rejected (insufficient funds)
        Payment->>Payment: FAILED + PaymentFailed outbox (same tx, no ledger event)
        Payment-->>Agent: 409
    else debit ok
        Payment->>Account: credit(dest) SERVICE JWT
        alt credit fails
            Payment->>Account: compensate credit source
            Payment->>Payment: FAILED + PaymentFailed outbox<br/>debitApplied=true → DEBIT_FAILED/COMPENSATE
            Payment-->>Agent: 409
        else success
            Payment->>Payment: COMPLETED + PaymentCompleted outbox (same tx)
            Payment->>Kafka: relay payment-events<br/>idempotent, acked after send
            Kafka->>Ledger: consume append<br/>source -amount + dest +amount<br/>idempotent by paymentId
            Kafka->>Notify: consume record confirmation
            Payment-->>Agent: 200 {paymentId}
        end
    end
    Note over Ledger,Notify: Opening entries Flyway-seeded from DemoConstants<br/>→ invariant holds at t0, no Kafka needed

    rect rgb(254, 243, 199)
        Note over Ledger: LEDGER_FAULT_SKIP_APPEND=true<br/>consume/ack but skip append<br/>→ deterministic missing leg
    end
```

1. `payment-service` runs the saga: `debit(source)` → `credit(dest)`, each with a
   **distinct** `idempotencyKey`; on credit failure it compensates (credits
   source back).
2. On success it writes a `PaymentCompleted` outbox row (same local DB tx),
   relayed to Kafka topic `payment-events` (idempotent, acked after send).
3. On failure the FAILED payment and a `PaymentFailed` outbox row commit in the
   same local DB tx (nothing is rolled back) and the API returns 409. The event
   carries `debitApplied` so the ledger only records the
   `DEBIT_FAILED`/`COMPENSATE` pair when the debit had actually moved money; a
   rejected debit (e.g. insufficient funds) records nothing.
4. `ledger-service` consumes and appends `source -amount` + `dest +amount`
   (idempotent by `paymentId`). `notification-service` consumes and records a
   confirmation.
5. Opening ledger entries are Flyway-seeded from `DemoConstants`, so the
   reconciliation invariant holds at t0 with no Kafka dependency.

## Money & consistency
- `Money` is a `BigDecimal` (scale 2, HALF_UP), serialized as a JSON **string**.
- Ownership enforced at `account-service` (`loadOwned`); `payment-service`
  propagates the caller JWT.
- `LEDGER_FAULT_SKIP_APPEND=true` makes the ledger consume/ack but skip the
  append — the deterministic "missing leg" break for the demo.

## Agent

```mermaid
graph LR
    UserReq["User message<br/>what is my balance?"] --> Planner

    subgraph PlannerLayer["Planner layer"]
        Llm["LlmPlanner<br/>google-genai<br/>AGENT_PROVIDER=gemini<br/>spring.ai.google.genai.*<br/>GoogleGenAiChatModel 1.1.8"]
        Kw["KeywordPlanner<br/>deterministic fallback"]
        Llm -->|unreachable / no key / unparseable| Kw
    end

    PlannerLayer --> Plan2["Plan DAG<br/>Step[]"]
    Plan2 --> Exec2["Executor<br/>topoSort → roleGate → approvalGate"]
    Exec2 --> Workers["Workers<br/>typed RestClient<br/>Authorization: Bearer < JWT >"]

    Workers --> GW2["Gateway? No — direct<br/>account-service<br/>ledger-service<br/>payment-service"]

    Exec2 --> Pending["pendingSteps?<br/>yes → reply + approval<br/>no → workers → reply"]

    Pending -->|approved on retry| Exec2

    Kw -.->|always available| Plan2

    style Llm fill:#2563eb,stroke:#1e40af,color:#fff,stroke-width:2px
    style Kw fill:#f59e0b,stroke:#b45309,color:#fff,stroke-width:2px
```

One agent, two role surfaces. Primary planner is `LlmPlanner` using Spring AI''s
`google-genai` starter (`AGENT_PROVIDER=gemini` default, key via
`AGENT_GEMINI_API_KEY`, model via `AGENT_GEMINI_MODEL`, wired through
`spring.ai.google.genai.*`); local Ollama is an alternative provider
(`AGENT_PROVIDER=ollama` + `SPRING_AI_OLLAMA_BASE_URL`). Spring AI 1.1.x ships the
Google GenAI starter (`GoogleGenAiChatModel`); the stack pins the 1.1.8 BOM with
Boot 3.5.16 / Spring Cloud 2025.0.3. With no key, the starter''s sentinel default
(`not-set`) keeps the context boot-safe and `LlmPlanner` drops to keyword-only.
The deterministic `KeywordPlanner` is the mandatory safety net — any LLM failure
(unreachable model, missing key, unparseable output) falls back to it, so the
demo never hard-fails. The executor runs
the plan DAG; workers are typed REST clients over the real backend.
Mutating steps become `pendingSteps` requiring explicit approval; the frontend
re-calls with the same `plan` + approved `stepIds` and idempotency key.
`reconcileAccount` (ops) replies with the root cause, a 12-entry evidence
trail, and a proposed corrective journal entry (not executed).

Architecture decisions (Spring AI + custom harness = ADR-0007, and any other ADRs):
see [our architecture decision records](../infra/docs/adrs/).

