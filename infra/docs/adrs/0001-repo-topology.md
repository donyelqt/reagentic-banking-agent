# ADR-0001: Monorepo topology for the microservice architecture

## Status
Accepted

## Date
2026-08-19

## Owner
Doniele (decision-maker & infrastructure owner)

## Context
The Cloud Elite internship program requires the banking platform to be built
as **microservices**. Given that constraint, the remaining topology decision
is how to store and version those services: one repository (monorepo) or one
repository per service (polyrepo/multi-repo).

Team and delivery context:
- Small team (8 people), internship program with a **~1-week development
  window per engagement** (demo-day deadline). Delivery velocity is the
  dominant constraint — every hour of setup or coordination overhead is
  measured against a fixed clock.
- Every service depends on a shared module (`common`: JWT, error contracts,
  DTOs) — the dependency graph is dense, not sparse.
- Local development is docker-compose based (see ADR-0002): one command boots
  all services. CI is a single reactor build (see ADR-0006).
- Deployment cadence is a single platform release, not independent
  per-service releases.

## Decision
Store all services in a single Git repository (`reagentic-banking-agent`)
with the Maven reactor as the module boundary: `common`, `auth-service`,
`account-service`, `payment-service`, `ledger-service`,
`notification-service`, `ai-agent`, `gateway`, plus `frontend/` and `infra/`.

## Rationale
- **Atomic cross-module changes.** A contract change in `common` and its
  consumers in `auth-service` and `payment-service` land in one commit, one
  review, one CI run. In a polyrepo, that same change is a version bump in
  `common`, followed by N coordinated PRs across N repositories with a
  compatibility window between them.
- **Onboarding overhead scales with repository count — and that overhead is
  revenue-relevant.** A new engineer clones one repo, runs one
  `docker-compose up`, and reads one README. Polyrepo onboarding means 8+
  clones, 8+ setups, and a mental map of which repo owns what contract. For
  an 8-person team with no release engineering, this overhead is paid
  repeatedly by every member on every engagement: onboarding delays erode
  development momentum, delivery slips, and fewer projects are completed on
  time — and for a consulting-style engagement model, fewer completed
  projects means less revenue.
- **One build graph.** The Maven reactor compiles in dependency order,
  catches intra-service breakage at build time, and keeps all modules on one
  version (`1.0.0`). Polyrepo forces version drift management and
  cross-repo build orchestration.
- **Tooling precedent at scale.** Google, Microsoft, and Meta operate
  monorepos for large microservice estates. Their primary motivation differs
  (cross-cutting refactors at massive scale), but it confirms the pattern is
  workable long-term; our adoption is justified by our own constraints above,
  not by imitation.

## Architecture & Infrastructure Judgement
The judgement behind this ADR, stated directly:

- **Why I stand behind this decision.** Topology is the decision that shapes
  everything downstream — builds, CI, deployment, onboarding. A wrong call
  here is paid *inside* the ~1-week window, not before it; the Rationale
  above is the mechanism, this is the conviction.
- **What this decision accepts, consciously.** A shared blast radius (one bad
  service blocks the whole build) and a faster-growing git history. Both are
  accepted tradeoffs with mitigations in Consequences — not hidden costs.
- **Boundary of the judgement.** This holds while the team ships one platform
  on one cadence. If services ever release independently or teams split by
  ownership, the monorepo cost flips sign and the topology must be revisited —
  the ADR says so rather than pretending the answer is permanent.

## Alternatives Considered

### Polyrepo (one Git repository per service)
- Pros: Independent release cadence per service; per-team code ownership and
  security boundaries; smaller repo working sets.
- Cons: Every shared-module change becomes a coordinated multi-repo release;
  onboarding cost multiplies per repo; CI must be duplicated or orchestrated
  per repo; no atomic cross-service change; requires release engineering we
  do not have.
- Rejected: The team has a single release train, no team-level ownership
  boundaries, and dense shared dependencies — every polyrepo advantage is
  unused and every disadvantage is paid.

### Hybrid (shared repo per domain, e.g. `backend/` + `frontend/` repos)
- Pros: Middle ground; reduces working-set size.
- Cons: Splits the dependency graph at an arbitrary line — `common` changes
  still cross the boundary; reintroduces coordinated releases between the two
  repos without solving the original problem.
- Rejected: Adds coordination cost without removing any monorepo downside
  that actually matters at this size.

## Consequences
- One clone, one CI pipeline, one version — onboarding and delivery stay
  fast (the team's core constraint).
- Working set is larger than a single service repo, but the full reactor
  builds in ~10 s (verified, see ADR-0006), so the cost is negligible.
- Ownership and blast radius are shared: a mistake in one service blocks the
  whole build. Mitigated by the CI gate on `main`/`donieledev` (ADR-0006)
  and per-service directories + contract tests.
- Git history will grow faster than a per-service repo — an accepted
  tradeoff (see the boundary in the Judgement section).