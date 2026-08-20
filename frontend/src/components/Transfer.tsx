import { useState } from 'react'
import { agentChat } from '../api'
import type { AccountView, AgentResponse } from '../types'
import ApprovalModal from './ApprovalModal'

const AMOUNT_PATTERN = /^\d+(\.\d{1,2})?$/

type Phase = 'idle' | 'planning' | 'approving' | 'executing'

function typeWord(type: string): string | null {
  const t = type.toLowerCase()
  if (t.includes('checking')) return 'checking'
  if (t.includes('savings')) return 'savings'
  return null
}

export default function Transfer({ accounts, onDone }: { accounts: AccountView[]; onDone: () => void }) {
  const [from, setFrom] = useState(accounts[0]?.accountId ?? '')
  const [to, setTo] = useState(accounts[1]?.accountId ?? '')
  const [amount, setAmount] = useState('')
  const [msg, setMsg] = useState<{ kind: 'ok' | 'err'; text: string } | null>(null)
  const [phase, setPhase] = useState<Phase>('idle')
  const [last, setLast] = useState<AgentResponse | null>(null)

  function validate(): string | null {
    const a = amount.trim()
    if (!from || !to) return 'Choose both a source and a destination account.'
    if (from === to) return 'Source and destination must be different accounts.'
    if (!AMOUNT_PATTERN.test(a)) return 'Enter a valid amount, e.g. 250.00 (up to 2 decimal places).'
    if (parseFloat(a) <= 0) return 'Amount must be greater than zero.'
    return null
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    if (phase !== 'idle') return
    const err = validate()
    if (err) {
      setMsg({ kind: 'err', text: err })
      return
    }
    const fromWord = typeWord(accounts.find((a) => a.accountId === from)?.type ?? '')
    const toWord = typeWord(accounts.find((a) => a.accountId === to)?.type ?? '')
    if (!fromWord || !toWord) {
      setMsg({ kind: 'err', text: 'The transfer assistant currently supports Checking and Savings accounts.' })
      return
    }
    setMsg(null)
    setPhase('planning')
    try {
      const message = `transfer ${amount.trim()} from ${fromWord} to ${toWord}`
      const res = await agentChat({ message })
      if (res.pendingSteps.length === 0) {
        setMsg({ kind: 'err', text: res.reply || 'The agent could not prepare this transfer. Try again.' })
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
    setPhase('executing')
    try {
      const ids = last.pendingSteps.map((s) => s.stepId)
      const res = await agentChat({ plan: last.plan, approval: ids })
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
        text: `Transfer complete. Payment ${result?.data?.paymentId ?? ''} status ${status ?? 'COMPLETED'}.`
      })
      setAmount('')
      setLast(null)
      setPhase('idle')
      setTimeout(onDone, 1400)
    } catch (err: any) {
      setMsg({ kind: 'err', text: err.message })
      setPhase('idle')
    }
  }

  if (accounts.length === 0) {
    return (
      <div className="max-w-xl">
        <BackButton onDone={onDone} />
        <h1 className="text-4xl">New transfer</h1>
        <p className="text-muted mt-2">Move money between your accounts. The agent prepares each transfer, and you approve it before anything executes.</p>
        <div className="card p-6 mt-7 text-muted text-sm" role="status">
          Your accounts aren't available right now. Refresh the page to try again.
        </div>
      </div>
    )
  }

  const busy = phase === 'planning' || phase === 'executing'

  return (
    <div className="max-w-xl">
      <BackButton onDone={onDone} />
      <h1 className="text-4xl">New transfer</h1>
      <p className="text-muted mt-2">Move money between your accounts. The agent prepares each transfer, and you approve it before anything executes.</p>

      <form onSubmit={submit} className="card relative p-6 mt-7 space-y-6" aria-busy={busy}>
        <div className="grid sm:grid-cols-2 gap-4">
          {(['from', 'to'] as const).map((role) => (
            <fieldset key={role}>
              <legend className="label capitalize mb-2">{role === 'from' ? 'From' : 'To'}</legend>
              <div className="grid gap-2">
                {accounts.map((a) => {
                  const selected = (role === 'from' ? from : to) === a.accountId
                  const disabled = (role === 'from' ? a.accountId === to : a.accountId === from) && accounts.length > 1
                  return (
                    <button type="button" key={a.accountId} aria-pressed={selected} disabled={disabled || busy}
                      onClick={() => role === 'from' ? setFrom(a.accountId) : setTo(a.accountId)}
                      className={`text-left rounded-3xl border p-3.5 transition ${selected ? 'border-accent bg-accent-soft' : 'border-line hover:border-ink/30'} ${disabled ? 'opacity-40 cursor-not-allowed' : ''} disabled:pointer-events-none`}>
                      <div className="flex items-center justify-between">
                        <span className="font-medium capitalize">{a.type.toLowerCase()}</span>
                        {selected && <span className="w-4 h-4 rounded-full bg-accent grid place-items-center text-white text-[10px]">✓</span>}
                      </div>
                      <div className="text-sm text-muted mt-0.5">${a.balance}</div>
                    </button>
                  )
                })}
              </div>
            </fieldset>
          ))}
        </div>

        <div>
          <label htmlFor="transfer-amount" className="label">Amount (USD)</label>
          <div className="mt-2 flex items-center rounded-2xl border border-line focus-within:border-accent focus-within:shadow-[0_0_0_4px_var(--accent-soft)] bg-bg px-4 transition">
            <span aria-hidden="true" className="text-2xl text-muted font-display">$</span>
            <input id="transfer-amount" className="flex-1 bg-transparent outline-none py-3.5 px-2 text-2xl font-display" inputMode="decimal" placeholder="0.00" value={amount} onChange={(e) => setAmount(e.target.value)} />
          </div>
        </div>

        {msg && (
          <div role={msg.kind === 'err' ? 'alert' : 'status'} className={`text-sm rounded-xl px-3 py-2 ${msg.kind === 'ok' ? 'tag-pos' : 'tag-neg'}`}>
            {msg.text}
          </div>
        )}

        <button type="submit" disabled={busy} className="btn btn-accent w-full !py-3.5 text-base disabled:opacity-60">
          {busy
            ? <span className="w-5 h-5 border-2 border-white/40 border-t-white rounded-full spin" />
            : 'Review & confirm'}
        </button>
      </form>

      {phase !== 'idle' && last && last.pendingSteps.length > 0 && (
        <ApprovalModal
          steps={last.pendingSteps}
          accounts={accounts}
          backdrop="blur"
          busy={phase === 'executing'}
          onApprove={onApprove}
          onCancel={() => { setLast(null); setMsg(null); setPhase('idle') }}
        />
      )}
    </div>
  )
}

function BackButton({ onDone }: { onDone: () => void }) {
  return (
    <button onClick={onDone} className="text-muted text-sm flex items-center gap-1.5 hover:text-ink transition mb-6">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M15 18l-6-6 6-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/></svg>
      Back
    </button>
  )
}