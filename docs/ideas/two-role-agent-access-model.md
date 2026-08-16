# Two-Role Agent Access Model

## Problem Statement
How might we make the agent's back-office reconciliation analyst framing legitimate
without rebuilding architecture, so it is coherent to a demo-day judge and safe with customer data?

## Recommended Direction
One system, two roles. A customer (USER) keeps the ownership-scoped experience they have
today (accounts, balances, supervised transfers) untouched. A bank employee (EMPLOYEE)
gets an ops console: /api/agent is EMPLOYEE-only (a USER gets 403), and the agent uses
EMPLOYEE-gated internal read endpoints to investigate ANY account break with root-cause
analysis plus a 12-entry evidence trail, presented as a proposed corrective journal entry.

The hero demo moment is the reconciliation root-cause diagnosis. The customer transfer plus
approval gate and the employee console are the supporting structure that proves segregation
of duties and that the agent never acts unilaterally.

Access is enforced at two layers (gateway hasRole plus service matcher) so the cross-account
read cannot leak to customers. The agent is EMPLOYEE-only end to end, so its workers can
safely call the internal reads.

## Key Assumptions to Validate
- Judges reward evidence-backed agentic reasoning over consumer UX polish.
- Demo machine runs Ollama, or deterministic mode is presented knowingly.
- Double-guarded internal read is safe enough for a single-user demo.
- Rubric tolerates internal framing while the user-facing shell remains.

## MVP Scope
- Seed ops@bank.dev (EMPLOYEE).
- Gateway: hasRole EMPLOYEE on /api/agent (USER 403).
- ai-agent: SecurityConfig requires EMPLOYEE on /api/agent.
- account plus ledger: EMPLOYEE-only internal read endpoints, double-guarded.
- Agent uses internal reads (caller always EMPLOYEE).
- Frontend: decode role, gate Agent tab, ops-console copy plus account selector.
- Customer path unchanged.

## Not Doing
- Real corrective-journal writes to ledger (post-demo ADR).
- Admin or user-search UX (demo has one user).
- LangGraph runtime (hand-rolled executor suffices).
- Two separate apps (one system plus roles is correct).
- OpenAI planner (dropped; Ollama primary plus keyword fallback).

## Open Questions
- Should the hero approve be a live pending step or narrative-only via transfer flow?
- Where does Ollama run on demo day (local laptop or deterministic upgrade path)?

## Status (implemented Aug 16)
All MVP scope items shipped. Resolution of the open questions:
- **Approve:** live. Agent transfers produce `pendingSteps`; the console shows
  the confirmation modal and re-calls with the same plan + idempotency key.
- **Ollama:** optional on the demo laptop via `SPRING_AI_OLLAMA_BASE_URL`;
  unset → deterministic keyword planner (presented knowingly as the upgrade
  path). Both paths are unit-tested for the reconcile reply.
- Hero reply now surfaces the 12-entry evidence trail plus the proposed
  corrective journal entry (Dr/Cr pair, marked NOT executed).
- Customer-facing `/api/ledger/{id}` is ownership-scoped via account-service
  (delegated check) — the cross-account read exists only on the EMPLOYEE
  internal endpoints.

## Follow-up (implemented Aug 16): customer-facing agent
Same `/api/agent` endpoint serves both roles; the executor enforces a
per-role tool matrix from the verified JWT role claim:

| Tool | USER (customer) | EMPLOYEE (ops) |
|---|---|---|
| `getBalance` / `listTransactions` | ownership-scoped endpoints | internal endpoints, any account |
| `transferFunds` | ✅ approval-gated, exactly-once | ❌ denied (investigation-only) |
| `reconcileAccount` | ❌ denied (ops only) | ✅ evidence + corrective entry |

The denial replies are explicit ("requires the EMPLOYEE role" /
"investigation-only"), which turns the guardrails into demo beats. The
EMPLOYEE-only gate on the internal read endpoints is unchanged — the
cross-account read still cannot leak to customers.
