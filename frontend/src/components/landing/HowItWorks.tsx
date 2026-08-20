import { ScrollReveal } from '../ScrollReveal'
import { SectionHeading } from './SectionHeading'

const steps = [
  {
    n: '01',
    title: 'Say it',
    body: 'Type a plain sentence — "move $50 to savings". The agent classifies intent and amount, then drafts a plan. Nothing moves yet.'
  },
  {
    n: '02',
    title: 'Approve it',
    body: 'Every money-moving action is prepared and staged for you. A transfer is never executed silently — it waits for a human yes.'
  },
  {
    n: '03',
    title: 'Trace it',
    body: 'Approved actions land in an immutable ledger as entries — debit, credit, running balance. Every cent is traceable, in real time.'
  }
]

export function HowItWorks() {
  return (
    <section id="how" className="scroll-mt-24 px-6 md:px-10 py-24 md:py-32 max-w-7xl mx-auto w-full">
      <SectionHeading
        eyebrow="How it works"
        title="A sentence in. A checked ledger out."
        sub="The loop is small and legible: the agent proposes, you approve, the ledger records."
        center
      />
      <div className="mt-16 grid md:grid-cols-3 gap-5">
        {steps.map((s, i) => (
          <ScrollReveal key={s.n} delay={i * 130}>
            <div className="relative glass-dark rounded-3xl p-7 h-full">
              <span className="font-display text-5xl bg-gradient-to-b from-[#7C8BFF] to-[#19C2F0] bg-clip-text text-transparent opacity-90">{s.n}</span>
              <h3 className="mt-5 text-white text-xl font-semibold">{s.title}</h3>
              <p className="mt-3 text-white/60 leading-relaxed text-[15px]">{s.body}</p>
              {i < 2 && (
                <svg className="hidden md:block absolute -right-[22px] top-1/2 -translate-y-1/2 z-10 text-white/25" width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <path d="M5 12h14M13 6l6 6-6 6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              )}
            </div>
          </ScrollReveal>
        ))}
      </div>
    </section>
  )
}