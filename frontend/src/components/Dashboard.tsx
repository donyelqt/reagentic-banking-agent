import { useEffect, useState } from 'react'
import { getLedger } from '../api'
import type { AccountView, LedgerEntry } from '../types'
import { useCountUp } from '../lib/useCountUp'

export default function Dashboard({ accounts, onTransfer }: { accounts: AccountView[]; onTransfer: () => void }) {
  const [activity, setActivity] = useState<LedgerEntry[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    Promise.all(accounts.map((a) => getLedger(a.accountId).then((r: any) => r.data ?? []).catch(() => [])))
      .then((lists) => {
        if (cancelled) return
        const all = lists.flat().sort((a: LedgerEntry, b: LedgerEntry) => (b.createdAt ?? 0) - (a.createdAt ?? 0))
        setActivity(all.slice(0, 6))
      })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [accounts])

  return (
    <div>
      <div className="flex items-end justify-between mb-8 flex-wrap gap-4">
        <div>
          <p className="label">Good day, demo</p>
          <h1 className="text-4xl mt-1">Your accounts</h1>
        </div>
        <button onClick={onTransfer} className="btn btn-accent">New transfer</button>
      </div>

      <div className="grid md:grid-cols-2 gap-5">
        {accounts.map((a, i) => <AccountCard key={a.accountId} account={a} index={i} />)}
      </div>

      <div className="card mt-6 p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl">Recent activity</h2>
          <span className="chip">Live ledger</span>
        </div>
        {loading ? <ActivitySkeleton /> : activity.length === 0 ? <p className="text-muted text-sm">No movements yet.</p> :
          <ul className="divide-y divide-line">
            {activity.map((e) => <ActivityRow key={e.entryId} e={e} />)}
          </ul>}
      </div>
    </div>
  )
}

function AccountCard({ account, index }: { account: AccountView; index: number }) {
  const num = useCountUp(parseFloat(account.balance || '0'))
  const amt = num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  return (
    <div className="card p-6 view-in" style={{ animationDelay: `${index * 80}ms` }}>
      <div className="flex items-center justify-between">
        <span className="chip capitalize">{account.type.toLowerCase()}</span>
        <span className="text-xs text-muted font-mono">{account.accountId}</span>
      </div>
      <div className="mt-5 font-display text-4xl">${amt}</div>
      <div className="mt-4 h-1.5 rounded-full bg-[#EDEBE3] overflow-hidden">
        <div className="h-full rounded-full" style={{ width: `${Math.min(100, (parseFloat(account.balance || '0') / 2000) * 100)}%`, background: 'linear-gradient(90deg,#2D43F5,#6A4BFF)' }} />
      </div>
    </div>
  )
}

function ActivityRow({ e }: { e: LedgerEntry }) {
  const credit = e.type === 'CREDIT' || e.type === 'OPENING'
  const label = e.type === 'OPENING' ? 'Opening balance' : e.type === 'DEBIT' ? 'Transfer out' : e.type === 'CREDIT' ? 'Transfer in' : e.type
  return (
    <li className="py-3.5 flex items-center justify-between">
      <div className="flex items-center gap-3">
        <span className={`w-9 h-9 rounded-full grid place-items-center ${credit ? 'bg-[rgba(12,166,120,.12)] text-pos' : 'bg-[rgba(229,72,77,.12)] text-neg'}`}>{credit ? '↓' : '↑'}</span>
        <div>
          <div className="text-sm font-medium">{label}</div>
          <div className="text-xs text-muted">{e.paymentId ? `ref ${e.paymentId}` : 'ledger'}</div>
        </div>
      </div>
      <div className={`font-display ${credit ? 'text-pos' : 'text-neg'}`}>{e.signedAmount}</div>
    </li>
  )
}

function ActivitySkeleton() {
  return (
    <div className="space-y-3">
      {[0, 1, 2].map((i) => (
        <div key={i} className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-full shimmer" />
          <div className="flex-1 h-3 rounded shimmer" />
          <div className="w-12 h-3 rounded shimmer" />
        </div>
      ))}
    </div>
  )
}