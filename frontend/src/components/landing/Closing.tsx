import { ScrollReveal } from '../ScrollReveal'
import { Brand } from '../Brand'

const footerLinks = [
  { label: 'How it works', href: '#how' },
  { label: 'The product', href: '#product' },
  { label: 'Security', href: '#security' }
]

export function Closing({ onEnter }: { onEnter: () => void }) {
  return (
    <div>
      <section className="px-6 md:px-10 py-24 md:py-32 max-w-4xl mx-auto w-full text-center">
        <ScrollReveal>
          <span className="chip glass-dark !bg-black/40 !text-white/80 !border-white/15">
            <span className="w-1.5 h-1.5 rounded-full bg-pos" /> Demo-ready
          </span>
          <h2 className="mt-6 text-white text-4xl md:text-6xl">
            Your private bank is one{' '}
            <span className="bg-gradient-to-r from-[#7C8BFF] via-[#B7A3FF] to-[#7CE0C0] bg-clip-text text-transparent">sentence</span>{' '}
            away.
          </h2>
          <p className="mt-6 text-white/60 text-lg max-w-xl mx-auto leading-relaxed">
            Type your first instruction. Approve the agent's plan. Watch the ledger move.
          </p>
        </ScrollReveal>
        <ScrollReveal delay={140}>
          <div className="mt-10 flex flex-wrap items-center justify-center gap-4">
            <button onClick={onEnter} className="btn btn-accent cta-pulse text-base">
              Enter the bank
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M5 12h14M13 6l6 6-6 6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" /></svg>
            </button>
            <span className="text-white/45 text-sm">No card. No paperwork. Demo-ready.</span>
          </div>
        </ScrollReveal>
      </section>

      <footer className="border-t border-white/10 px-6 md:px-10 py-10 max-w-7xl mx-auto w-full">
        <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
          <div>
            <Brand tone="dark" />
            <p className="mt-3 text-white/45 text-sm max-w-xs leading-relaxed">
              An agentic private bank demo — plain-sentence commands, human-approved transfers, immutable ledger.
            </p>
          </div>
          <nav aria-label="Footer" className="flex flex-wrap gap-x-7 gap-y-3 text-sm">
            {footerLinks.map((l) => (
              <a key={l.href} href={l.href} className="text-white/60 hover:text-white transition">
                {l.label}
              </a>
            ))}
            <a href="#problem" className="text-white/60 hover:text-white transition">Why</a>
          </nav>
        </div>
        <div className="mt-8 pt-6 border-t border-white/10 flex flex-col md:flex-row items-start md:items-center justify-between gap-3 text-sm text-white/35">
          <span>© 2026 Reagentic · Agentic banking demo</span>
          <span>Fictional balances for demonstration — every cent is simulated.</span>
        </div>
      </footer>
    </div>
  )
}