import { useEffect, useState } from 'react'
import { getLedger, downloadStatementCsv, downloadStatementExcel, classifySpending } from '../api'
import type { AccountView, CategorySpend, LedgerEntry } from '../types'
import { useCountUp } from '../lib/useCountUp'
import { CategoryChart } from './CategoryChart';
import { SpendingTrendChart } from './SpendingTrendChart';

export default function Dashboard({ accounts, onTransfer, onViewAll }: { accounts: AccountView[]; onTransfer: () => void; onViewAll: () => void }) {
  const [allActivity, setAllActivity] = useState<LedgerEntry[]>([])
  const [activity, setActivity] = useState<LedgerEntry[]>([])
  const [categorySummary, setCategorySummary] = useState<CategorySpend[] | null>(null)
  const [classifyError, setClassifyError] = useState(false)
  const [dlError, setDlError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    Promise.all(accounts.map((a) => getLedger(a.accountId).then((r: any) => r.data ?? []).catch(() => [])))
      .then((lists) => {
        if (cancelled) return
        const all = lists.flat().sort((a: LedgerEntry, b: LedgerEntry) => (b.createdAt ?? 0) - (a.createdAt ?? 0))
        setAllActivity(all)
        setActivity(all.slice(0, 6))
        return all
      })
      .then((all) => {
        if (cancelled || !all) return
        classifySpending(all).then(setCategorySummary).catch(() => { if (!cancelled) setClassifyError(true) })
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
        {accounts.map((a, i) => <AccountCard key={a.accountId} account={a} index={i} onDownloadError={setDlError} />)}
      </div>

      <div className="grid lg:grid-cols-2 gap-6 mt-6">
        <CategoryChart
          summary={categorySummary}
          error={classifyError}
          onRetry={() => {
            setCategorySummary(null)
            setClassifyError(false)
            classifySpending(allActivity).then(setCategorySummary).catch(() => setClassifyError(true))
          }}
        />
        <SpendingTrendChart data={allActivity} />
      </div>

      {dlError && (
        <div className="tag-neg mt-6 rounded-xl px-4 py-2.5 flex items-center justify-between text-sm">
          <span>{dlError}</span>
          <button className="font-medium underline" onClick={() => setDlError(null)}>Dismiss</button>
        </div>
      )}

      <div className="card p-6 mt-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl">Recent activity</h2>
          <div className="flex items-center gap-2">
            <span className="chip">Latest</span>
            <button onClick={onViewAll} className="text-sm font-medium text-accent hover:underline">View all</button>
          </div>
        </div>
        {loading ? <ActivitySkeleton /> : activity.length === 0 ? <p className="text-muted text-sm">No movements yet.</p> :
          <ul className="divide-y divide-line">
            {activity.map((e) => <ActivityRow key={e.entryId} e={e} />)}
          </ul>}
      </div>
    </div>
  )
}

function AccountCard({ account, index, onDownloadError }: { account: AccountView; index: number; onDownloadError: (msg: string | null) => void }) {
  const num = useCountUp(parseFloat(account.balance || '0'))
  const amt = num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  const run = (fn: () => Promise<void>, label: string) => {
    onDownloadError(null)
    fn().catch(() => onDownloadError(`Couldn't download ${label}. Try again.`))
  }
  return (
    <div className="card p-6 view-in" style={{ animationDelay: `${index * 80}ms` }}>
      <div className="flex items-center justify-between">
        <span className="chip capitalize">{account.type.toLowerCase()}</span>
        <span className="text-xs text-muted font-mono">{account.accountId}</span>
      </div>
      <div className="mt-5 font-display text-4xl">${amt}</div>
      <div className="mt-6 flex items-center justify-end gap-2">
        <button
          onClick={() => run(() => downloadStatementCsv(account.accountId), 'CSV')}
          className="text-xs font-medium text-[#8A6D1A] bg-gold/15 hover:bg-gold/25 px-3 py-1.5 rounded-full transition-colors"
          title="Download statement as plain CSV"
        >
          CSV
        </button>
        <button
          onClick={() => run(() => downloadStatementExcel(account.accountId), 'Excel')}
          className="text-xs font-medium text-accent bg-accent/10 hover:bg-accent/20 px-3 py-1.5 rounded-full transition-colors"
          title="Download the same statement as a styled Excel workbook"
        >
          Excel
        </button>
      </div>
    </div>
  )
}

function ActivityRow({ e }: { e: LedgerEntry }) {
  const credit = e.type === 'CREDIT' || e.type === 'OPENING'
  const label = e.type === 'OPENING' ? 'Opening balance' : e.type === 'DEBIT' ? 'Transfer out' : e.type === 'CREDIT' ? 'Transfer in' : e.type
  const amt = parseFloat(e.signedAmount || '0')
  const formatted = (credit ? '+' : '−') + '$' + Math.abs(amt).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  const date = new Date(e.createdAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
  return (
    <li className="py-3.5 flex items-center justify-between">
      <div className="flex items-center gap-3">
        <span aria-hidden="true" className={`w-9 h-9 rounded-full grid place-items-center ${credit ? 'bg-[rgba(12,166,120,.12)] text-pos' : 'bg-[rgba(229,72,77,.12)] text-neg'}`}>{credit ? '↑' : '↓'}</span>
        <div>
          <div className="text-sm font-medium">{label}</div>
          <div className="text-xs text-muted">{date} · {e.paymentId ? `ref ${e.paymentId}` : 'ledger'}</div>
        </div>
      </div>
      <div className={`font-display ${credit ? 'text-pos' : 'text-neg'}`}>{formatted}</div>
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