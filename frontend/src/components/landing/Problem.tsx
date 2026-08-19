import { ScrollReveal } from '../ScrollReveal'
import { SectionHeading } from './SectionHeading'

const pains = [
  {
    n: '01',
    title: 'Banking is buried in portals',
    body: 'Transfers in one app, statements in another, reconciliation in a third. Your money is the only thing they share.'
  },
  {
    n: '02',
    title: 'AI talks, it never acts',
    body: 'Chatbots summarize dashboards and suggest next steps. The step is yours — in a form, on a deadline, before cutoff.'
  },
  {
    n: '03',
    title: 'Trust is a checkbox',
    body: 'Automation asks once, then runs unchecked. One stale rate, one silent retry, one unapproved transfer — and the ledger is wrong.'
  }
]

export function Problem() {
  return (
    <section id="problem" className="scroll-mt-24 px-6 md:px-10 py-24 md:py-32 max-w-7xl mx-auto w-full">
      <div className="grid lg:grid-cols-2 gap-14 items-start">
        <SectionHeading
          eyebrow="The problem"
          title={
            <>
              Banking grew up around humans in front of <span className="bg-gradient-to-r from-[#7C8BFF] via-[#B7A3FF] to-[#7CE0C0] bg-clip-text text-transparent">portals</span>. The money never moved by itself.
            </>
          }
          sub="Real money lives in ledger entries — every debit matched by a credit, every cent accounted for. The portal is just the window. Software that makes you do the ledger's work by hand is software that lost its job."
        />
        <div className="space-y-4">
          {pains.map((p, i) => (
            <ScrollReveal key={p.n} delay={i * 110}>
              <div className="glass-dark rounded-3xl p-6 md:p-7 flex gap-5 hover:border-white/25 transition-colors">
                <span className="font-display text-2xl bg-gradient-to-b from-[#7C8BFF] to-[#19C2F0] bg-clip-text text-transparent shrink-0 leading-none pt-1">{p.n}</span>
                <div>
                  <h3 className="text-white text-lg font-semibold">{p.title}</h3>
                  <p className="mt-2 text-white/60 leading-relaxed text-[15px]">{p.body}</p>
                </div>
              </div>
            </ScrollReveal>
          ))}
        </div>
      </div>
    </section>
  )
}