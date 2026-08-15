// Reagentic brand mark.
// The glyph is a deliberate evolution of Accenture's signature ">" — here a forward
// double-chevron: a faint trailing "re-" echo chasing a bold cobalt->cyan "agentic"
// chevron. It reads as Accenture's ">", encodes reinvention ("re"), and signals the
// agent stepping forward. The looping advance = reinvention in motion.

type BrandTone = 'dark' | 'light'

export function Brand({ tone = 'dark' }: { tone?: BrandTone }) {
  const toneClass = tone === 'dark' ? 'brand--dark' : 'brand--light'
  return (
    <div className={`brand ${toneClass}`}>
      <span className="brand-mark" aria-hidden="true">
        <svg viewBox="0 0 60 40" fill="none" xmlns="http://www.w3.org/2000/svg">
          <defs>
            <linearGradient id="reagenticStroke" x1="12" y1="8" x2="40" y2="32" gradientUnits="userSpaceOnUse">
              <stop stopColor="#2D43F5" />
              <stop offset="1" stopColor="#19C2F0" />
            </linearGradient>
          </defs>
          <path
            className="bm-echo"
            d="M12 9 L24 20 L12 31"
            stroke="url(#reagenticStroke)"
            strokeWidth="6"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          <path
            d="M26 9 L40 20 L26 31"
            stroke="url(#reagenticStroke)"
            strokeWidth="6.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </span>
      <span className="brand-word">
        <span className="brand-re">re</span><span className="brand-agentic">agentic</span>
      </span>
    </div>
  )
}