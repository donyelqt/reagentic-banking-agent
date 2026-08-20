import { useState } from 'react'
import { agentChat } from '../api'
import type { AccountView, AgentResponse } from '../types'
import ApprovalModal from './ApprovalModal'
import { maskedId } from '../utils'

const AMOUNT_PATTERN = /^\d+(\.\d{1,2})?$/

type Phase = 'idle' | 'planning' | 'approving' | 'executing'

/* ---------- presentational helpers (bank-grade formatting) ---------- */

function typeLabel(type: string): string {
  const t = (type || '').toLowerCase()
  return t.charAt(0).toUpperCase() + t.slice(1)
}

function isSupported(type: string): boolean {
  const t = (type || '').toLowerCase()
  return t === 'checking' || t === 'savings'
}

function formatMoney(value: number | string): string {
  const n = typeof value === 'string' ? parseFloat(value) : value
  if (!Number.isFinite(n)) return '$0.00'
  return '$' + n.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function typeWord(type: string): string | null {
  const t = type.toLowerCase()
  if (t.includes('checking')) return 'checking'
  if (t.includes('savings')) return 'savings'
  return null
}

/* ---------- account option row ---------- */

function AccountOption({
  account,
  selected,
  disabled,
  busy,
  showAvailable,
  onSelect,
}: {
  account: AccountView
  selected: boolean
  disabled: boolean
  busy: boolean
  showAvailable?: boolean
  onSelect: () => void
}) {
  const supported = isSupported(account.type)
  const unusable = disabled || !supported
  return (
    <button
      type="button"
      role="radio"
      aria-checked={selected}
      disabled={unusable || busy}
      onClick={onSelect}
      className={`group w-full flex items-center gap-3 rounded-2xl border p-3 text-left transition
        ${selected ? 'border-accent bg-accent-soft' : 'border-line hover:border-ink/30'}
        ${unusable ? 'opacity-40 cursor-not-allowed' : 'cursor-pointer'}`}
    >
      <span
        className={`w-5 h-5 rounded-full border-2 grid place-items-center shrink-0 transition ${
          selected ? 'border-accent bg-accent' : 'border-line'
        }`}
        aria-hidden="true"
      >
        {selected && <span className="w-2 h-2 rounded-full bg-white" />}
      </span>

      <span
        aria-hidden="true"
        className="w-9 h-9 rounded-xl bg-bg grid place-items-center font-display text-sm font-semibold text-muted shrink-0"
      >
        {typeLabel(account.type).charAt(0)}
      </span>

      <span className="min-w-0 flex-1">
        <span className="block font-medium capitalize leading-tight">{account.type.toLowerCase()}</span>
        <span className="block text-xs text-muted font-mono">{maskedId(account.accountId)}</span>
      </span>

      <span className="text-right shrink-0">
        <span className="block font-display">{formatMoney(account.balance)}</span>
        {showAvailable && selected && <span className="block text-[11px] text-pos mt-0.5">Available</span>}
        {!supported && <span className="block text-[11px] text-muted mt-0.5">Unavailable</span>}
      </span>
    </button>
  )
}

/* ---------- page ---------- */

export default function Transfer({ accounts, onDone }: { accounts: AccountView[]; onDone: () => void }) {
  const supported = accounts.filter((a) => isSupported(a.type))
  const [from, setFrom] = useState(supported[0]?.accountId ?? accounts[0]?.accountId ?? '')
  const [to, setTo] = useState(supported[1]?.accountId ?? accounts[1]?.accountId ?? '')
  const [amount, setAmount] = useState('')
  const [msg, setMsg] = useState<{ kind: 'ok' | 'err'; text: string } | null>(null)
  const [phase, setPhase] = useState<Phase>('idle')
  const [last, setLast] = useState<AgentResponse | null>(null)

  const busy = phase === 'planning' || phase === 'executing'
  const fromAcc = accounts.find((a) => a.accountId === from)
  const toAcc = accounts.find((a) => a.accountId === to)
  const available = (() => {
    const b = parseFloat(fromAcc?.balance ?? '0')
    return Number.isFinite(b) ? b : 0
  })()
  const amountValid = AMOUNT_PATTERN.test(amount.trim())
  const amountValue = amount.trim() ? parseFloat(amount.trim()) : 0

  function validate(): string | null {
    if (!from || !to) return 'Choose a source and a destination account.'
    if (from === to) return 'Choose two different accounts.'
    const a = amount.trim()
    if (!a) return 'Enter an amount to transfer.'
    if (!amountValid) return 'Enter a valid amount, like 250.00.'
    if (amountValue <= 0) return 'Amount must be greater than zero.'
    if (amountValue > available) return `That exceeds your available balance (${formatMoney(available)}).`
    return null
  }

  function swap() {
    if (busy) return
    setFrom(to)
    setTo(from)
    setMsg(null)
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    if (phase !== 'idle') return
    const err = validate()
    if (err) {
      setMsg({ kind: 'err', text: err })
      return
    }
    const fromWord = typeWord(fromAcc?.type ?? '')
    const toWord = typeWord(toAcc?.type ?? '')
    if (!fromWord || !toWord) {
      setMsg({ kind: 'err', text: 'This transfer supports Checking and Savings accounts only.' })
      return
    }
    setMsg(null)
    setPhase('planning')
    try {
      const message = `transfer ${amount.trim()} from ${fromWord} to ${toWord}`
      const res = await agentChat({ message })
      if (res.pendingSteps.length === 0) {
        setMsg({ kind: 'err', text: res.reply || 'We could not prepare this transfer. Please try again.' })
        setPhase('idle')
        return
      }
      setLast(res)
      setPhase('approving')
    } catch (err: any) {
      setMsg({ kind: 'err', text: err.message })
      setPhase('idle')
    }
  }

  async function onApprove() {
    if (!last) return
    if (!last.approvalId) {
      setMsg({ kind: 'err', text: 'This session expired. Please review the transfer again and re-confirm.' })
      setLast(null)
      setPhase('idle')
      return
    }
    setPhase('executing')
    try {
      const ids = last.pendingSteps.map((s) => s.stepId)
      const res = await agentChat({ approvalId: last.approvalId, approval: ids })
      const result = res.results.find((r) => r.stepId === ids[0]) ?? res.results[0]
      if (result && !result.ok) {
        setMsg({ kind: 'err', text: result.error ?? 'The transfer failed.' })
        setPhase('idle')
        return
      }
      const status = result?.data?.status
      if (status === 'FAILED') {
        setMsg({ kind: 'err', text: result?.data?.reason ?? 'The transfer failed.' })
        setPhase('idle')
        return
      }
      setMsg({
        kind: 'ok',
        text: `Sent ${formatMoney(amountValue)} from ${typeLabel(fromAcc?.type ?? '')} (${maskedId(
          fromAcc?.accountId ?? '',
        )}) to ${typeLabel(toAcc?.type ?? '')} (${maskedId(toAcc?.accountId ?? '')}).`,
      })
      setAmount('')
      setLast(null)
      setPhase('idle')
      setTimeout(onDone, 1500)
    } catch (err: any) {
      setMsg({ kind: 'err', text: err.message })
      setPhase('idle')
    }
  }

  if (accounts.length === 0) {
    return (
      <div className="max-w-4xl">
        <BackButton onDone={onDone} />
        <h1 className="text-4xl">New transfer</h1>
        <p className="text-muted mt-2">Move money between your own accounts. Review the details and confirm before anything moves.</p>
        <div className="card p-6 mt-7 text-muted text-sm" role="status">
          Your accounts aren't available right now. Refresh the page to try again.
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-4xl relative">
      <BackButton onDone={onDone} />
      <h1 className="text-4xl">New transfer</h1>
      <p className="text-muted mt-2">
        Move money between your own accounts. You stay in control — nothing moves until you review and confirm.
      </p>

      <div className="grid lg:grid-cols-[minmax(0,1fr)_320px] gap-6 items-start mt-7">
        {/* left: form */}
        <form onSubmit={submit} className="card relative p-6 space-y-6 order-2 lg:order-1" aria-busy={busy}>
          {/* From */}
          <fieldset>
            <legend className="label mb-2">From account · money leaves here</legend>
            <div role="radiogroup" aria-label="Source account" className="grid gap-2">
              {accounts.map((a) => (
                <AccountOption
                  key={a.accountId}
                  account={a}
                  selected={from === a.accountId}
                  disabled={a.accountId === to}
                  busy={busy}
                  showAvailable
                  onSelect={() => {
                    setFrom(a.accountId)
                    setMsg(null)
                  }}
                />
              ))}
            </div>
          </fieldset>

          {/* swap */}
          <div className="flex justify-center -my-1 relative z-10">
            <button
              type="button"
              onClick={swap}
              disabled={busy}
              aria-label="Swap source and destination"
              className="w-10 h-10 rounded-full bg-surface border border-line shadow-soft grid place-items-center text-muted hover:text-accent hover:border-accent transition disabled:opacity-50"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path d="M7 10l-3 3 3 3M4 13h11" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                <path d="M17 14l3-3-3-3M20 11H9" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </button>
          </div>

          {/* To */}
          <fieldset>
            <legend className="label mb-2">To account · money arrives here</legend>
            <div role="radiogroup" aria-label="Destination account" className="grid gap-2">
              {accounts.map((a) => (
                <AccountOption
                  key={a.accountId}
                  account={a}
                  selected={to === a.accountId}
                  disabled={a.accountId === from}
                  busy={busy}
                  onSelect={() => {
                    setTo(a.accountId)
                    setMsg(null)
                  }}
                />
              ))}
            </div>
          </fieldset>

          {/* Amount */}
          <div>
            <label htmlFor="transfer-amount" className="label">Amount (USD)</label>
            <div className="mt-2 flex items-center rounded-2xl border border-line focus-within:border-accent focus-within:shadow-[0_0_0_4px_var(--accent-soft)] bg-bg px-4 transition">
              <span aria-hidden="true" className="text-2xl text-muted font-display">$</span>
              <input
                id="transfer-amount"
                className="flex-1 bg-transparent outline-none py-3.5 px-2 text-2xl font-display tabular-nums"
                inputMode="decimal"
                placeholder="0.00"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                aria-describedby={from ? 'avail-hint' : undefined}
              />
            </div>
            {from && (
              <p id="avail-hint" className="text-xs text-muted mt-1.5">
                Available to transfer from {typeLabel(fromAcc?.type ?? '')}:{' '}
                <span className="font-medium text-ink">{formatMoney(available)}</span>
              </p>
            )}
          </div>

          {msg && (
            <div
              role={msg.kind === 'err' ? 'alert' : 'status'}
              className={`text-sm rounded-xl px-3 py-2 ${msg.kind === 'ok' ? 'tag-pos' : 'tag-neg'}`}
            >
              {msg.text}
            </div>
          )}

          <button type="submit" disabled={busy} className="btn btn-accent w-full !py-3.5 text-base disabled:opacity-60">
            {busy ? (
              <>
                <span className="w-5 h-5 border-2 border-white/40 border-t-white rounded-full spin" aria-hidden="true" />
                <span>Preparing…</span>
              </>
            ) : (
              <>
                Review transfer
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <path d="M5 12h14M13 6l6 6-6 6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              </>
            )}
          </button>
        </form>

        {/* right: summary + reassurance rail */}
        <aside className="space-y-4 order-1 lg:order-2 lg:sticky lg:top-6" aria-live="polite">
          <div className="card p-5">
            <p className="label">You're sending</p>
            <div className={`font-display text-3xl mt-1 tabular-nums ${amountValid ? 'text-ink' : 'text-muted/70'}`}>
              {formatMoney(amountValue)}
            </div>
            <div className="h-px bg-line my-4" />
            <div className="space-y-2">
              <div className="flex items-center justify-between gap-2">
                <span className="text-xs text-muted shrink-0">From</span>
                <span className="text-sm font-medium truncate">
                  {from ? `${typeLabel(fromAcc?.type ?? '')} ${maskedId(fromAcc?.accountId ?? '')}` : '—'}
                </span>
              </div>
              <div className="flex items-center justify-between gap-2">
                <span className="text-xs text-muted shrink-0">To</span>
                <span className="text-sm font-medium truncate">
                  {to ? `${typeLabel(toAcc?.type ?? '')} ${maskedId(toAcc?.accountId ?? '')}` : '—'}
                </span>
              </div>
            </div>
          </div>

          <div className="card p-5 bg-bg">
            <div className="flex items-start gap-3">
              <span aria-hidden="true" className="w-8 h-8 rounded-full grid place-items-center bg-accent/10 text-accent shrink-0">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                  <path d="M12 3l7 3v5c0 4.5-3 7.5-7 9-4-1.5-7-4.5-7-9V6l7-3z" stroke="currentColor" strokeWidth="2" strokeLinejoin="round" />
                  <path d="M9 12l2 2 4-4" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              </span>
              <div>
                <p className="text-sm font-medium">You're in control</p>
                <p className="text-xs text-muted mt-1">Nothing moves until you review and confirm. You can cancel anytime.</p>
              </div>
            </div>
          </div>
        </aside>
      </div>

      {phase !== 'idle' && last && last.pendingSteps.length > 0 && (
        <ApprovalModal
          steps={last.pendingSteps}
          accounts={accounts}
          backdrop="blur"
          busy={phase === 'executing'}
          onApprove={onApprove}
          onCancel={() => {
            setLast(null)
            setMsg(null)
            setPhase('idle')
          }}
        />
      )}
    </div>
  )
}

function BackButton({ onDone }: { onDone: () => void }) {
  return (
    <button onClick={onDone} className="text-muted text-sm flex items-center gap-1.5 hover:text-ink transition mb-6">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M15 18l-6-6 6-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
      Back
    </button>
  )
}
