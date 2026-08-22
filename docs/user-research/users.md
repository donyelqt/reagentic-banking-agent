# Users

**Interviewed by:** Doniele Arys Antonio (team GC discussion).
**Participants:** Khrystelline, Chris Owyn.
**Method:** Capstone problem framing — stated pains and desired outcomes.
**Bias note:** Small sample (capstone team). Treat as directional, not surveyed truth.

> **Doniele's prompt to the group:** "former business ad, fin mngt student ka pala noh
> btw, since ex-business student ka and the industry for our capstone here iz banking, right?
> so for u? if business ad student ka pa rin or naging professional ka sa industry, mapa fintech
> or banking? anong task na u wish na dapat automated na using agentic AI/AI or parang di mo
> gusto na ulit uliten ahahha something like that, for our mvp purposes"

---

## Khrystelline — IT student (formerly Financial Management)

**On the acute pain:**
> "Yung balancing😆"

> "Sa accounting dalawa yung mostly natatagalan kami noon either yung sa mortgage or yung balancing. Same din sa accounting essentials kaksks."

**On scope reality:**
> "Defi pwede din"
> "Charrot one week lang pala capstone"

### Job framing
The acute job is **account reconciliation / balancing** — not transactions, not reporting, but making sure the books match the real money. It shows up in two coursework contexts (mortgage, "Accounting Essentials"), so the pain is repetitive, not one-off. For customer services is "malawak"

---

## Chris Owyan — CS student

**Echoing Khrystelline:**
> "Pwede kaya yung customer service? Pero malawak din kasi customer service eh"

### Job framing
Same core job as Khrystelline: kill the manual balancing step. Echoes Khrystelline's framing, so this is a consistent team signal, not a one-off complaint.

---

## What the capstone solves

**Problem:** Manual account reconciliation eats hours across coursework, and the "books don't balance" step is the bottleneck — not the entry or the report.

**Solution (scoped):** An agent that detects mismatched accounts, surfaces the delta + root cause (`MISSING_DEBIT_LEG` / `MISSING_CREDIT_LEG`, `INSUFFICIENT_FUNDS`), and proposes a corrective journal entry — human-approved, never auto-applied.

```
User → "Yung balancing" → reconcile endpoint → LedgerService (idempotent, PESSIMISTIC_WRITE)
                                      → PaymentService (compensation-aware failure leg)
                                      → agent returns delta + reason, awaits approval
```

**Non-goals (this capstone):** Full customer-service triage ("malawang") and DEFI expansion — both acknowledged but explicitly out of scope for a one-week capstone window. Building the balancing core first; CS/DEI is the next extension.

## Evidence quality
- **Quotes:** direct, verbatim, from Khrystelline and Chris in the team GC (interviewed by Doniele Arys Antonio).
- **Severity:** conversational self-report, not measured frequency. Confirmed across two participants, so directional signal is stable, but unverified in production.
