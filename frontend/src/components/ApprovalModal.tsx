import { useEffect, useRef } from 'react'
import type { Step } from '../types'

interface ApprovalModalProps {
  steps: Step[]
  onApprove: () => void
  onCancel: () => void
  busy?: boolean
}

function toolLabel(tool: string) {
  const spaced = tool.replace(/([A-Z])/g, ' $1')
  return spaced.charAt(0).toUpperCase() + spaced.slice(1)
}

function formatAmount(value: unknown) {
  const n = Number(value)
  return Number.isFinite(n) ? n.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : String(value)
}

interface StepDetail {
  route?: string
  amount?: string
}

function formatStepDetails(args: Step['args']): StepDetail | null {
  try {
    const parsed = typeof args === 'string' ? JSON.parse(args) : args
    if (parsed?.amount) {
      const route = parsed.from ? `${parsed.from} → ${parsed.to ?? '?'}` : undefined
      return { route, amount: formatAmount(parsed.amount) }
    }
    if (parsed?.accountId) {
      return { route: String(parsed.accountId) }
    }
  } catch {
    // fall through to the generic label
  }
  return null
}

export default function ApprovalModal({ steps, onApprove, onCancel, busy }: ApprovalModalProps) {
  const approveRef = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    approveRef.current?.focus()
  }, [])

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="approval-title"
      aria-busy={busy}
      className="absolute inset-0 z-20 grid place-items-end sm:place-items-center p-3 backdrop-in bg-[#0A0B14]/80"
      onKeyDown={(e) => { if (e.key === 'Escape' && !busy) onCancel() }}
    >
      <div className="card p-5 w-full max-w-md modal-in shadow-lift">
        <div className="flex items-center gap-2 mb-3">
          <span aria-hidden="true" className="w-7 h-7 rounded-full grid place-items-center bg-gold/15 text-gold font-display">!</span>
          <h2 id="approval-title" className="font-display text-lg">Confirmation required</h2>
        </div>
        <p className="text-sm text-muted mb-3">The agent prepared these steps. Approve to execute.</p>
        <ul className="space-y-2 mb-4">
          {steps.map((s) => {
            const detail = formatStepDetails(s.args)
            return (
              <li key={s.stepId} className="flex items-center justify-between gap-3 rounded-xl border border-line bg-bg px-3 py-2 text-sm">
                <span className="font-medium shrink-0">{toolLabel(s.tool)}</span>
                {detail ? (
                  <>
                    {detail.route && <span className="text-muted truncate text-right min-w-0">{detail.route}</span>}
                    {detail.amount && <span className="font-display font-semibold shrink-0">${detail.amount}</span>}
                  </>
                ) : (
                  <span className="text-muted truncate text-right">No details</span>
                )}
              </li>
            )
          })}
        </ul>
        <div className="flex gap-2">
          <button ref={approveRef} onClick={onApprove} disabled={busy} className="btn btn-accent flex-1">
            {busy ? 'Executing…' : 'Approve & execute'}
          </button>
          <button onClick={onCancel} disabled={busy} className="btn btn-ghost flex-1">Cancel</button>
        </div>
      </div>
    </div>
  )
}