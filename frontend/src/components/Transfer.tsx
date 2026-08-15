import { useState } from 'react'
import { transfer } from '../api'
import type { AccountView } from '../types'

export default function Transfer({ accounts, onDone }: { accounts: AccountView[]; onDone: () => void }) {
  const [from, setFrom] = useState(accounts[0]?.accountId ?? '')
  const [to, setTo] = useState(accounts[1]?.accountId ?? '')
  const [amount, setAmount] = useState('')
  const [msg, setMsg] = useState<{ kind: 'ok' | 'err'; text: string } | null>(null)
  const [loading, setLoading] = useState(false)

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    if (!from || !to || !amount || parseFloat(amount) <= 0) {
      setMsg({ kind: 'err', text: 'Enter a valid amount.' }); return
    }
    setLoading(true); setMsg(null)
    try {
      const res = await transfer({ sourceAccountId: from, destinationAccountId: to, amount, idempotencyKey: crypto.randomUUID() })
      setMsg({ kind: 'ok', text: `Sent. Status: ${res?.data?.status ?? 'completed'}` })
      setAmount('')
      setTimeout(onDone, 1100)
    } catch (err: any) { setMsg({ kind: 'err', text: err.message }) }
    finally { setLoading(false) }
  }

  return (
    <div className="max-w-xl">
      <button onClick={onDone} className="text-muted text-sm flex items-center gap-1.5 hover:text-ink transition mb-6">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none"><path d="M15 18l-6-6 6-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/></svg>
        Back
      </button>
      <h1 className="text-4xl">New transfer</h1>
      <p className="text-muted mt-2">Move money between your accounts. Approved by you, executed by the agent.</p>

      <form onSubmit={submit} className="card p-6 mt-7 space-y-6">
        <div className="grid sm:grid-cols-2 gap-4">
          {(['from', 'to'] as const).map((role) => (
            <div key={role}>
              <label className="label capitalize">{role}</label>
              <div className="mt-2 grid gap-2">
                {accounts.map((a) => {
                  const selected = (role === 'from' ? from : to) === a.accountId
                  return (
                    <button type="button" key={a.accountId} onClick={() => role === 'from' ? setFrom(a.accountId) : setTo(a.accountId)}
                      className={`text-left rounded-3xl border p-3.5 transition ${selected ? 'border-accent bg-accent-soft' : 'border-line hover:border-ink/30'}`}>
                      <div className="flex items-center justify-between">
                        <span className="font-medium capitalize">{a.type.toLowerCase()}</span>
                        {selected && <span className="w-4 h-4 rounded-full bg-accent grid place-items-center text-white text-[10px]">✓</span>}
                      </div>
                      <div className="text-sm text-muted mt-0.5">${a.balance}</div>
                    </button>
                  )
                })}
              </div>
            </div>
          ))}
        </div>

        <div>
          <label className="label">Amount (USD)</label>
          <div className="mt-2 flex items-center rounded-2xl border border-line focus-within:border-accent focus-within:shadow-[0_0_0_4px_var(--accent-soft)] bg-bg px-4 transition">
            <span className="text-2xl text-muted font-display">$</span>
            <input className="flex-1 bg-transparent outline-none py-3.5 px-2 text-2xl font-display" inputMode="decimal" placeholder="0.00" value={amount} onChange={(e) => setAmount(e.target.value)} />
          </div>
        </div>

        {msg && <div className={`text-sm rounded-xl px-3 py-2 ${msg.kind === 'ok' ? 'text-pos bg-[rgba(12,166,120,.12)]' : 'text-neg bg-[rgba(229,72,77,.1)]'}`}>{msg.text}</div>}

        <button type="submit" disabled={loading} className="btn btn-accent w-full !py-3.5 text-base disabled:opacity-60">
          {loading ? <span className="w-5 h-5 border-2 border-white/40 border-t-white rounded-full spin" /> : 'Send transfer'}
        </button>
      </form>
    </div>
  )
}