import { useState } from 'react'
import { transfer } from '../api'
import type { AccountView } from '../types'

export default function Transfer({ accounts }: { accounts: AccountView[] }) {
  const [from, setFrom] = useState(accounts[0]?.accountId ?? '')
  const [to, setTo] = useState(accounts[1]?.accountId ?? '')
  const [amount, setAmount] = useState('')
  const [msg, setMsg] = useState<{ kind: 'ok' | 'err'; text: string } | null>(null)
  const [loading, setLoading] = useState(false)

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    if (!from || !to || !amount) return
    setLoading(true)
    setMsg(null)
    try {
      const res = await transfer({
        sourceAccountId: from,
        destinationAccountId: to,
        amount,
        idempotencyKey: crypto.randomUUID()
      })
      setMsg({ kind: 'ok', text: `Transferred. Status: ${res?.data?.status ?? 'done'}` })
      setAmount('')
    } catch (err: any) {
      setMsg({ kind: 'err', text: err.message })
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={submit} className="space-y-4 max-w-md">
      <h2 className="text-lg font-semibold">Transfer</h2>
      <div>
        <label className="block text-sm text-slate-600">From</label>
        <select className="w-full border rounded p-2" value={from} onChange={(e) => setFrom(e.target.value)}>
          {accounts.map((a) => (
            <option key={a.accountId} value={a.accountId}>
              {a.type} ({a.accountId}) — {a.balance}
            </option>
          ))}
        </select>
      </div>
      <div>
        <label className="block text-sm text-slate-600">To</label>
        <select className="w-full border rounded p-2" value={to} onChange={(e) => setTo(e.target.value)}>
          {accounts.map((a) => (
            <option key={a.accountId} value={a.accountId}>
              {a.type} ({a.accountId}) — {a.balance}
            </option>
          ))}
        </select>
      </div>
      <div>
        <label className="block text-sm text-slate-600">Amount (USD)</label>
        <input
          className="w-full border rounded p-2"
          value={amount}
          inputMode="decimal"
          placeholder="0.00"
          onChange={(e) => setAmount(e.target.value)}
        />
      </div>
      {msg && (
        <div className={msg.kind === 'ok' ? 'text-green-600 text-sm' : 'text-red-600 text-sm'}>
          {msg.text}
        </div>
      )}
      <button
        type="submit"
        disabled={loading}
        className="bg-indigo-600 text-white rounded p-2 px-4 hover:bg-indigo-700 disabled:opacity-50"
      >
        {loading ? 'Sending…' : 'Transfer'}
      </button>
    </form>
  )
}
