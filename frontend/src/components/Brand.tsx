export function Brand({ tone = 'dark' }: { tone?: 'dark' | 'light' }) {
  const text = tone === 'dark' ? 'text-white' : 'text-ink'
  return (
    <div className="flex items-center gap-2.5 select-none">
      <span
        className="relative w-8 h-8 rounded-xl overflow-hidden shadow-[0_8px_20px_-6px_rgba(45,67,245,.8)]"
        style={{ background: 'conic-gradient(from 200deg,#2D43F5,#6A4BFF,#0CA678,#2D43F5)' }}
      >
        <span className="absolute inset-[3px] rounded-[10px] bg-[#0A0B14]/80 grid place-items-center">
          <span className="w-2.5 h-2.5 rounded-full bg-white" />
        </span>
      </span>
      <span className={`font-display text-lg font-semibold tracking-tight ${text}`}>
        Reagentic
      </span>
    </div>
  )
}