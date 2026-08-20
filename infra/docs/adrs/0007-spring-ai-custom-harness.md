# ADR-0007: Spring AI + custom agent harness over unofficial LangChain/LangGraph ports

## Status
Accepted

## Date
2026-08-20

## Owner
Doniele (decision-maker & product/engineering owner)

## Context
The ai-agent service must orchestrate LLM-backed "agent" behavior: planning user intents into
executable steps, executing multi-step plans against the banking domain, classifying transactions,
and — critically — enforcing money-movement guardrails (approval gates, role gates, idempotency,
tool whitelists). Two families of libraries were considered to provide the LLM plumbing:

1. **Spring AI** — the official Spring project for AI applications, released under the Spring
   umbrella with a versioned BOM aligned to Spring Boot.
2. **LangChain4j / LangGraph4j** — third-party, community-driven ports of the LangChain/LangGraph
   (Python/JS) ecosystems to the JVM. Neither is sponsored or versioned by the Spring team.

The decision also covers *how much* agent framework to adopt: full third-party agent abstractions
(graphs, chains, memory, tool routers) vs. a thin custom harness over a chat-model provider.

Key requirements:
- Stay inside the opinionated Spring Boot ecosystem (DI, config binding, actuator, testing) with
  no framework-impedance mismatch between the agent code and the rest of the system.
- Keep trust boundaries — approval of money movement, role checks, idempotency keys — in plain,
  visible, auditable code rather than inside a black-box framework.
- Deterministic, testable behavior with no LLM API key configured (the demo and CI must run
  without external model access).
- Provider flexibility: local (Ollama) and cloud (Gemini) models, switched by configuration.

## Decision
Use **Spring AI** (`spring-ai-starter-model-ollama`, `spring-ai-starter-model-google-genai`,
managed by the `spring-ai-bom`) as the model-access layer, and implement a **small custom agent
harness** on top of it — do **not** use LangChain4j or LangGraph4j.

The harness today is:
- `ChatClientProvider` — resolves the single active `ChatClient` from `agent.provider`
  (`gemini`/`ollama`); when no API key is configured, LLM-backed components fall back to their
  deterministic implementations (never a hard failure).
- `KeywordPlanner` / `LlmPlanner` — deterministic intent-to-plan and LLM plan generation for the
  same tool contract; the LLM path hard-forces `confirmationRequired=true` and fresh idempotency
  keys on `transferFunds` steps.
- `Executor` — executes plans as a **minimal custom DAG**: steps are nodes, `dependsOn`
  declares edges, a topo-sort (with cycle guard) produces the execution order, and each node runs
  behind the approval gate; enforces the role gate (EMPLOYEE vs USER tool permissions) and stable
  idempotency keys; `reconcileDirect` exposes the same reconcile engine as a direct endpoint for
  the manual Ledger Console.
- `AgentWorkers` — the only place that calls domain services (accounts, ledger, payments) over
  HTTP; every backend response is unwrapped and validated here.
- `LlmTransactionClassifier` / keyword classifier — spending classification with a keyword
  fallback and order-drift detection.

## Alternatives Considered

### LangChain4j
- Pros: Rich community feature set (chains, memory, RAG scaffolding); popular in the JVM AI space;
  some abstractions resemble our planner/executor split.
- Cons: **Unofficial** third-party project — not part of the Spring release train; its own
  versioning and transitive dependencies frequently conflict with the Spring Boot-managed BOM;
  heavy abstraction layer that hides tool routing and guardrail behavior; auditing
  money-movement paths inside its abstractions is harder; API churn driven by its own roadmap,
  not ours.
- Rejected: the ecosystem mismatch and black-box guardrails outweigh the convenience for a
  banking domain where every money step must be provably gated.

### LangGraph4j
- Pros: Graph-based agent orchestration for multi-step, stateful workflows; closest conceptual
  fit to "agent" framing.
- Cons: **Unofficial port** of the Python/JS LangGraph with a small maintainer base and lagging
  release cadence; our plans are **already a DAG** — step nodes with `dependsOn` edges, topo-sorted
  and executed by our own `Executor` — so a full state-machine graph runtime (persistent state,
  checkpoints, conditional edges) is overkill; graph execution state would move the approval gate
  inside a framework; smallest community and least test ecosystem of the options.
- Rejected: maintenance and support risk plus unnecessary complexity for a plan shape we already
  serve with ~30 lines of topo-sort.

### Raw HTTP to model providers (no Spring AI)
- Pros: Zero extra dependencies.
- Cons: Duplicated client code per provider, no uniform `ChatClient` abstraction, manual JSON
  schema handling, no Spring-config property binding, no auto-configuration.
- Rejected: re-implements what Spring AI already provides, for no benefit.

### Spring AI only (no custom harness)
- Pros: Least code of all options.
- Cons: Planner/executor/guardrail logic would leak into chat glue code; the approval gate, role
  gate, and idempotency keys — the actual product — need a home with explicit unit-testable
  boundaries; Spring AI is intentionally a model-access layer, not an agent framework.
- Rejected: the harness is the product's core logic and deserves first-class, owned code.

## Consequences
- **Positive**
  - Single opinionated stack: the agent service uses the same DI, configuration, logging,
    actuator, and test patterns as every other service in the system; developers do not learn a
    parallel framework.
  - Dependency hygiene: Spring AI versions come from the `spring-ai-bom` aligned to Spring Boot;
    no third-party version conflicts, smaller transitive surface, easier security review.
  - Auditability: the approval gate, role gate, idempotency, and tool whitelist live in
    `Executor` as plain code with unit tests — provable behavior, not framework configuration.
  - Testability: 65 unit tests in ai-agent run with no network and no API keys; deterministic
    fallbacks keep CI and the demo functional offline.
  - Provider portability: switching or adding a model provider is a starter dependency + a
    `ChatClientProvider` branch; the harness and planners are provider-agnostic.
  - Upgrade alignment: Spring AI releases track Spring Boot releases, so upgrades ride the
    existing Boot upgrade path.
- **Negative / accepted**
  - The custom harness is our code to maintain (planner contracts, executor, classifier seams).
    Mitigated by keeping the Spring AI integration surface minimal (one `ChatClientProvider` seam
    and thin model-call methods), so API churn in Spring AI is contained.
  - Spring AI is itself young and fast-moving; early versions may reshape APIs. We accept this
    for the ecosystem alignment benefits and contain it at the seam.
  - We forgo community agent conveniences (memory, RAG scaffolding, multi-agent toolkits) — we
    build only what the product needs (planning, execution, classification), which today is small
    and deliberately so.

## Implementation Evidence
- `ai-agent/pom.xml` — `spring-ai-starter-model-ollama`, `spring-ai-starter-model-google-genai`
- `dev.reagentic.ai.agent.ChatClientProvider` — provider resolution + deterministic fallback
- `dev.reagentic.ai.agent.KeywordPlanner`, `LlmPlanner` — same tool contract, two plan paths
- `dev.reagentic.ai.agent.Executor` — approval gate, role gate, idempotency, topo-sort,
  `reconcileDirect`
- `dev.reagentic.ai.agent.AgentWorkers` — sole HTTP boundary to domain services
- `dev.reagentic.ai.agent.LlmTransactionClassifier` — LLM classification with keyword fallback
- `docs/architecture.md` — service topology and agent flow