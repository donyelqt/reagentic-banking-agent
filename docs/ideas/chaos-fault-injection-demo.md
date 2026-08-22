# Chaos Fault-Injection Demo

## Problem Statement

How might we show judges, in a 5-minute demo, that our agent can find a ledger break, prove it with evidence, and propose the fix — without waiting for a real bank to break on stage?

## Recommended Direction

Keep the scripted single-fault demo (`LEDGER_FAULT_SKIP_APPEND=true`): ledger consumes payment events but skips the append ("missing leg" break) → agent shows root cause + evidence trail → proposed journal entry → ops approves. This is Netflix Chaos Monkey–style fault injection: deliberate, realistic failure to prove recovery. It is the product's core loop (break → diagnose → propose → approve) made demonstrable on demand. Already implemented (application.yml + LedgerService) and right-sized: one env flag, zero new infra. Frame it honestly in the pitch: "we inject a realistic ledger break — the same discipline behind Netflix's Chaos Monkey."

## Key Assumptions to Validate

- [ ] Reconcile path is deterministic on stage (keyword fallback, no live LLM dependency) — verify end-to-end on the running stack
- [ ] Seed data and demo state are frozen/reproducible across rehearsals
- [ ] Judges read "fault injection" as sophistication, not a rigged demo — the honest one-line framing mitigates this
- [ ] The 5-minute walkthrough (hero script, Task 5) survives rehearsal without glitches

## MVP Scope

- The one scripted fault: missing-leg break, triggered by env flag
- The one demo script: break → diagnosis with evidence → proposal → approval
- One-line judge framing: "fault injection — the discipline behind Netflix's Chaos Monkey"

## Not Doing (and Why)

- No Chaos Monkey infrastructure (simian army, Gremlin, LitmusChaos) — violates "refine, don't add"; burns rehearsal time
- No random/latency/multi-fault injection — one rehearsed scenario beats five that can glitch
- No "we run chaos in production" claims — unverifiable on stage; honest answer is "fault injection for demo + tests, production chaos is roadmap"
- No CSV/other scope creep — playbook doctrine stands

## Open Questions

- Should the fault flag be toggled via a runtime endpoint instead of env restart for cleaner demo transitions? (Probably not worth it — env + restart is simpler and safer on stage.)
- Does the demo open with the fault already injected, or inject mid-flow for theater? (Recommend: pre-injected, agent discovers it — less on-stage risk.)