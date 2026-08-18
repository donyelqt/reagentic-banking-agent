import type { ReactNode } from 'react'
import { ScrollReveal } from '../ScrollReveal'

export function SectionHeading({
  eyebrow,
  title,
  sub,
  center = false
}: {
  eyebrow: string
  title: ReactNode
  sub?: string
  center?: boolean
}) {
  return (
    <ScrollReveal className={center ? 'text-center mx-auto max-w-2xl' : 'max-w-2xl'}>
      <span className="chip glass-dark !bg-black/40 !text-white/80 !border-white/15">
        <span className="w-1.5 h-1.5 rounded-full bg-pos" />
        {eyebrow}
      </span>
      <h2 className="mt-6 text-white text-3xl md:text-5xl">{title}</h2>
      {sub && <p className="mt-5 text-white/60 text-lg leading-relaxed">{sub}</p>}
    </ScrollReveal>
  )
}