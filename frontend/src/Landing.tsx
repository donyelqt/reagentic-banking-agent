import { Brand } from './components/Brand'
import { Reveal } from './components/Reveal'
import { Problem } from './components/landing/Problem'
import { HowItWorks } from './components/landing/HowItWorks'
import { Pillars } from './components/landing/Pillars'
import { Security } from './components/landing/Security'
import { Closing } from './components/landing/Closing'

export default function Landing({ onEnter }: { onEnter: () => void }) {
  return (
    <div className="aurora grain">
      <div className="orb orb-1" />
      <div className="orb orb-2" />
      <div className="orb orb-3" />
      <div className="grid-lines" />

      <div className="relative z-10">
        <div className="min-h-screen flex flex-col">
          <header className="flex items-center justify-between px-6 md:px-10 py-5">
            <Brand tone="dark" />
            <nav className="hidden md:flex items-center gap-8 text-sm text-white/70" aria-label="Primary">
              <a className="hover:text-white transition" href="#how">How it works</a>
              <a className="hover:text-white transition" href="#product">Product</a>
              <a className="hover:text-white transition" href="#security">Security</a>
            </nav>
            <button onClick={onEnter} className="btn btn-ghost !text-white !border-white/20 hover:!bg-white/10">
              Sign in
            </button>
          </header>

          <main className="flex-1 grid lg:grid-cols-2 items-center gap-12 px-6 md:px-10 max-w-7xl mx-auto w-full py-10">
            <div>
              <Reveal delay={0}>
                <span className="chip glass-dark !bg-black/40 !text-white/80 !border-white/15">
                  <span className="w-1.5 h-1.5 rounded-full bg-pos" /> Agentic private banking
                </span>
              </Reveal>
              <h1 className="mt-6 text-white text-5xl md:text-6xl lg:text-7xl">
                <Reveal delay={80}><span className="block">Banking that</span></Reveal>
                <Reveal delay={160}><span className="block">acts on your</span></Reveal>
                <Reveal delay={240}>
                  <span className="block bg-gradient-to-r from-[#7C8BFF] via-[#B7A3FF] to-[#7CE0C0] bg-clip-text text-transparent">
                    words.
                  </span>
                </Reveal>
              </h1>
              <Reveal delay={340}>
                <p className="mt-6 text-white/65 text-lg max-w-md leading-relaxed">
                  A private bank run by an agent you approve. Move money, reconcile accounts,
                  and trace every cent through a real-time ledger — all from a sentence.
                </p>
              </Reveal>
              <Reveal delay={440}>
                <div className="mt-9 flex flex-wrap items-center gap-4">
                  <button onClick={onEnter} className="btn btn-accent cta-pulse text-base">
                    Enter the bank
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M5 12h14M13 6l6 6-6 6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" /></svg>
                  </button>
                  <span className="text-white/45 text-sm">No card. No paperwork. Demo-ready.</span>
                </div>
              </Reveal>
            </div>

            <Reveal delay={300} className="relative">
              <div className="glass-dark rounded-[28px] p-6 w-full max-w-md ml-auto shadow-[0_40px_120px_-30px_rgba(45,67,245,.6)]">
                <div className="flex items-center justify-between mb-5">
                  <span className="text-white/70 text-sm">Today</span>
                  <span className="chip !bg-white/10 !border-white/15 !text-white/80">Demo preview</span>
                </div>
                <div className="text-white/55 text-sm">Total available</div>
                <div className="font-display text-5xl text-white mt-1">$1,550.00</div>
                <div className="mt-6 space-y-3">
                  <div className="flex items-start gap-3">
                    <span className="mt-0.5 w-7 h-7 rounded-full grid place-items-center bg-white/10 text-white text-xs">You</span>
                    <div className="text-white/80 text-sm">Move $50 to savings</div>
                  </div>
                  <div className="flex items-start gap-3">
                    <span className="mt-0.5 w-7 h-7 rounded-full grid place-items-center bg-accent text-white text-xs">A</span>
                    <div className="text-white/90 text-sm">
                      Prepared a transfer. <span className="text-white/55">Awaiting your approval.</span>
                    </div>
                  </div>
                </div>
                <div className="mt-6 flex gap-2">
                  <button className="btn btn-accent flex-1 !py-2.5">Approve</button>
                  <button className="btn !bg-white/10 !border-white/15 !text-white flex-1 !py-2.5">Edit</button>
                </div>
              </div>
            </Reveal>
          </main>

          <footer className="px-6 md:px-10 pb-10 max-w-7xl mx-auto w-full">
            <div className="flex flex-wrap gap-3">
              {['Plain-sentence commands', 'Human-approved transfers', 'Real-time immutable ledger'].map((f, i) => (
                <Reveal key={f} delay={520 + i * 90}>
                  <span className="chip glass-dark !bg-black/40 !text-white/80 !border-white/15">{f}</span>
                </Reveal>
              ))}
            </div>
          </footer>
        </div>

        <Problem />
        <HowItWorks />
        <Pillars />
        <Security />
        <Closing onEnter={onEnter} />
      </div>
    </div>
  )
}