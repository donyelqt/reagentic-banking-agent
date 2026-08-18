import { ScrollReveal } from '../ScrollReveal'
import { SectionHeading } from './SectionHeading'

const pillars = [
  {
    title: 'Agentic guardrails',
    body: 'Money-moving actions are staged as plans and executed only after explicit human approval. The agent proposes; the human disposes.',
    tag: 'Approval-gated by design'
  },
  {
    title: 'Dual-role security',
    body: 'Customers act on their own money; ops analysts supervise across the bank. Roles are enforced at the API, not just hidden in the UI.',
    tag: 'Customer · Ops Analyst'
  },
  {
    title: 'Event-driven ledger',
    body: 'Every approved action streams through Kafka into an append-only ledger with running balances. No double-entry drift, no stale CSV.',
    tag: 'Immutable · Real-time'
  },
  {
    title: 'Grounded AI',
    body: 'Intent is classified against a strict contract — amounts, categories, and confidence — with deterministic fallbacks when the model is unsure.',
    tag: 'Deterministic fallbacks'
  }
]

export function Pillars() {
  return (
    <section id="product" className="scroll-mt-24 px-6 md:px-10 py-24 md:py-32 max-w-7xl mx-auto w-full">
      <SectionHeading
        eyebrow="The product"
        title="A private bank run by an agent you approve"
        sub="Four engineering commitments make the agent worth trusting with money."
        center
      />
      <div className="mt-16 grid sm:grid-cols-2 gap-5">
        {pillars.map((p, i) => (
          <ScrollReveal key={p.title} delay={i * 90}>
            <div className="glass-dark rounded-3xl p-7 h-full flex flex-col hover:border-white/25 transition-colors">
              <h3 className="text-white text-xl font-semibold">{p.title}</h3>
              <p className="mt-3 text-white/60 leading-relaxed text-[15px] flex-1">{p.body}</p>
              <span className="chip mt-6 self-start !bg-white/5 !border-white/15 !text-white/70">{p.tag}</span>
            </div>
          </ScrollReveal>
        ))}
      </div>
    </section>
  )
}