# ADR-0006: CI/CD with GitHub Actions

## Status
Accepted

## Date
2026-08-19

## Context
The repository (`donyelqt/reagentic-banking-agent`) has no CI pipeline today. It is a
7-service Spring Boot monorepo (8 Maven modules: `common`, `auth-service`,
`account-service`, `payment-service`, `ledger-service`, `notification-service`,
`ai-agent`, `gateway`) plus a Vite/React frontend, developed by a small team
against a demo-day deadline (Cloud Elite submission). Deployment targets are
Azure (ACI for staging, AKS skeleton for production); local development runs
through `docker-compose`.

Requirements driving the decision:
- Every change must pass automated quality gates before merge (per the
  `ci-cd-and-automation` standard: lint/typecheck, unit tests, build).
- Zero infrastructure to operate — the team runs infra (docker-compose,
  Terraform, AKS manifests) but has no dedicated ops role: every hour spent
  maintaining a CI server is a dev hour taken from demo delivery.
- Gated branches: `main` (release) and `donieledev` (active dev branch).
- Future deploy path must support short-lived credentials (OIDC into Azure),
  no long-lived secrets in the repo.

## Decision
Use **GitHub Actions** with a single `ci.yml` workflow containing three jobs:

| Job | Gate | What it enforces |
|-----|------|------------------|
| `backend` | `./mvnw -B test` | Compiles + runs unit tests across all 8 Maven modules (JDK 17, Maven wrapper 3.9.9, cached `~/.m2`) |
| `frontend` | `npm ci` → `npm run typecheck` → `npm run build` → `npm audit --omit=dev --audit-level=high` | Type safety, production build, clean production dependency tree (Node 22, cached `node_modules`) |
| `docker-image` | `./mvnw -B package -DskipTests` → `docker build -f infra/Dockerfile --build-arg MODULE=auth-service` | Fat-jar naming contract (`${MODULE}-1.0.0.jar`) and the shared service Dockerfile stay valid |

Gates deliberately match what exists in the repository. The frontend has no
`lint` or `test` scripts today, so those gates are not fabricated; they are
added as follow-ups (see Consequences).

Runs on `pull_request` and `push` to `main` and `donieledev`, with
`concurrency.cancel-in-progress` so rapid pushes on the dev branch do not
queue stale builds. `permissions: contents: read` (least privilege).

## Alternatives Considered

### Jenkins
- Pros: Fully self-hosted, mature plugin ecosystem, no per-minute cost.
- Cons: Requires a dedicated server to keep patched, secured, and available —
  recurring maintenance hours (patching, TLS, storage, uptime, backups) that
  produce no new capability here; webhook plumbing to GitHub; no native OIDC
  federation.
- Rejected: Self-hosting tax with zero compensating requirement — no on-prem,
  compliance, or data-residency constraints force it, and hosted CI delivers
  identical gates without the maintenance burden.

### GitLab CI
- Pros: Native `.gitlab-ci.yml`, strong pipeline features, zero managed infra.
- Cons: Repository lives on GitHub; adopting GitLab means splitting
  source-of-truth from CI or migrating the repo entirely.
- Rejected: No reason to move the repo; would split tooling.

### Azure DevOps Pipelines
- Pros: Native Azure integration, excellent OIDC/Workload Identity support.
- Cons: Second toolchain to learn and configure; GitHub Actions already
  covers the Azure path via `azure/login` + OIDC.
- Rejected: Adds a platform with no benefit over Actions for a GitHub-hosted
  repo.

### CircleCI / Buildkite
- Pros: Fast parallel builds, polished UX.
- Cons: Paid tiers to gain anything over Actions; Buildkite requires
  self-hosted agents (reintroduces the Jenkins problem).
- Rejected: Premature cost for a demo-stage project.

## Consequences
- Every PR and push to `main`/`donieledev` is verified automatically; failed
  gates block merge once branch protection is configured.
- Maven and npm caches keep the pipeline under ~10 minutes per job.
- No secrets exist in the pipeline yet (nothing to deploy); when Azure
  deploys are added, credentials flow via GitHub OIDC federation → Azure,
  not static secrets (see ADR-0003 cloud skeleton).
- Known risk, tracked: `vite` (devDependency) has a high-severity dev-server
  advisory; the production dependency audit passes (0 vulnerabilities). Fix
  requires a vite major bump — deferred as a separate task so it can be
  verified against the build.
- Follow-ups (not fabricated gates, real gaps): frontend lint + unit test
  scripts; integration tests via Testcontainers with Kafka/Postgres service
  containers; full Docker image matrix per module; Dependabot for npm/Maven;
  branch protection rules requiring this workflow to pass.