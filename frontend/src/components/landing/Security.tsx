import { ScrollReveal } from '../ScrollReveal'
import { SectionHeading } from './SectionHeading'

const checks = [
  {
    title: 'Nothing moves without a yes',
    body: 'Transfers are staged as pending steps. The agent cannot, by design, complete an action it was not explicitly approved to complete.'
  },
  {
    title: 'Two roles, one truth',
    body: 'Customers operate their own accounts. Ops analysts see the whole bank through the console. Permissions are checked server-side.'
  },
  {
    title: 'Every cent is traceable',
    body: 'Each entry carries a running balance after it. Reconcile any moment by replaying the ledger — no hidden amendments.'
  }
]

export function Security() {
  return (
    <section id="security" className="scroll-mt-24 px-6 md:px-10 py-24 md:py-32 max-w-7xl mx-auto w-full">
      <div className="grid lg:grid-cols-2 gap-14 items-center">
        <div>
          <SectionHeading
            eyebrow="Security"
            title="A bank can delegate execution. It should never delegate consent."
            sub="The agent handles the busywork — drafting, reconciling, moving. The human holds the final say on every financial action."
          />
          <div className="mt-10 space-y-6">
            {checks.map((c, i) => (
              <ScrollReveal key={c.title} delay={i * 100}>
                <div className="flex gap-4">
                  <span className="mt-1 w-6 h-6 rounded-full grid place-items-center bg-pos/15 text-pos shrink-0">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M5 13l4 4L19 7" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" /></svg>
                  </span>
                  <div>
                    <h3 className="text-white font-semibold text-lg">{c.title}</h3>
                    <p className="mt-1.5 text-white/60 leading-relaxed text-[15px]">{c.body}</p>
                  </div>
                </div>
              </ScrollReveal>
            ))}
          </div>
        </div>

        <ScrollReveal delay={120} className="relative">
          <div className="glass-dark rounded-[28px] p-6 md:p-8 shadow-[0_40px_120px_-30px_rgba(45,67,245,.5)]">
            <div className="flex items-center justify-between mb-6">
              <span className="text-white/70 text-sm">Ledger · after approval</span>
              <span className="chip !bg-white/10 !border-white/15 !text-white/80">immutable</span>
            </div>
            <div className="space-y-4 text-sm font-mono">
              <div className="flex items-start gap-3">
                <span className="mt-0.5 w-7 h-7 rounded-full grid place-items-center bg-pos/20 text-pos text-xs shrink-0">+</span>
                <div className="min-w-0 flex-1">
                  <div className="text-white/90 truncate">credit · savings</div>
                  <div className="text-white/45 mt-0.5">Move $50 to savings · approved by you</div>
                </div>
                <span className="text-pos shrink-0">+$50.00</span>
              </div>
              <div className="h-px bg-white/10" />
              <div className="flex items-start gap-3">
                <span className="mt-0.5 w-7 h-7 rounded-full grid place-items-center bg-neg/20 text-neg text-xs shrink-0">−</span>
                <div className="min-w-0 flex-1">
                  <div className="text-white/90 truncate">debit · checking</div>
                  <div className="text-white/45 mt-0.5">Move $50 to savings · approved by you</div>
                </div>
                <span className="text-neg shrink-0">−$50.00</span>
              </div>
            </div>
            <div className="mt-6 rounded-2xl bg-black/30 border border-white/10 p-4 space-y-2 text-sm font-mono">
              <div className="flex justify-between text-white/60"><span>checking · balance after</span><span className="text-white/90">$950.00</span></div>
              <div className="flex justify-between text-white/60"><span>savings · balance after</span><span className="text-white/90">$550.00</span></div>
            </div>
          </div>
        </ScrollReveal>
      </div>
    </section>
  )
}