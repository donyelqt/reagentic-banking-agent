import { useEffect, useState } from 'react'
import { getLedger, downloadStatementCsv, downloadStatementExcel, classifyEntries } from '../api'
import type { AccountView, LedgerEntry } from '../types'
import { CATEGORY_COLORS } from '../lib/chartColors'

function money(v: number, sign = false): string {
  const abs = Math.abs(v).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  return (sign ? (v >= 0 ? '+' : '−') : v < 0 ? '−' : '') + '$' + abs
}

function label(category: string): string {
  return category.charAt(0).toUpperCase() + category.slice(1).toLowerCase()
}

function categoryColor(category: string): string {
  const names = ['Groceries', 'Dining', 'Transport', 'Utilities', 'Subscriptions', 'Shopping', 'Entertainment', 'Health', 'Travel', 'Income', 'Transfer', 'Other']
  const idx = names.indexOf(label(category))
  return CATEGORY_COLORS[(idx >= 0 ? idx : 11) % CATEGORY_COLORS.length]
}

export default function ActivityPage({ accounts }: { accounts: AccountView[] }) {
  const [accountId, setAccountId] = useState(accounts[0]?.accountId ?? '')
  const [entries, setEntries] = useState<LedgerEntry[]>([])
  const [categories, setCategories] = useState<Map<number, string>>(new Map())
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [dlError, setDlError] = useState<string | null>(null)
  const [reload, setReload] = useState(0)

  useEffect(() => {
    if (!accountId) return
    let cancelled = false
    setLoading(true)
    setError(false)
    getLedger(accountId)
      .then(async (r: any) => {
        if (cancelled) return
        const list: LedgerEntry[] = r.data ?? []
        setEntries(list)
        const map = await classifyEntries(list)
        if (!cancelled) setCategories(map)
      })
      .catch(() => { if (!cancelled) setError(true) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [accountId, reload])

  const run = (fn: () => Promise<void>, labelName: string) => {
    setDlError(null)
    fn().catch(() => setDlError(`Couldn't download ${labelName}. Try again.`))
  }

  return (
    <div>
      <div className="flex items-end justify-between mb-6 flex-wrap gap-4">
        <div>
          <p className="label">Full history, straight from the ledger</p>
          <h1 className="text-4xl mt-1">Activity</h1>
        </div>
        {accounts.length > 1 && (
          <label className="flex items-center gap-3">
            <span className="label">Account</span>
            <select className="field !w-auto" value={accountId} onChange={(e) => setAccountId(e.target.value)}>
              {accounts.map((a) => <option key={a.accountId} value={a.accountId}>{a.type} ({a.accountId})</option>)}
            </select>
          </label>
        )}
      </div>

      <div className="flex items-center justify-end gap-2 mb-4">
        <button
          onClick={() => run(() => downloadStatementCsv(accountId), 'CSV')}
          className="text-xs font-medium text-[#8A6D1A] bg-gold/15 hover:bg-gold/25 px-3 py-1.5 rounded-full transition-colors"
          title="Download statement as plain CSV"
        >
          CSV
        </button>
        <button
          onClick={() => run(() => downloadStatementExcel(accountId), 'Excel')}
          className="text-xs font-medium text-accent bg-accent/10 hover:bg-accent/20 px-3 py-1.5 rounded-full transition-colors"
          title="Download the same statement as a styled Excel workbook"
        >
          Excel
        </button>
      </div>

      {dlError && (
        <div className="tag-neg rounded-xl px-4 py-2.5 flex items-center justify-between text-sm mb-4">
          <span>{dlError}</span>
          <button className="font-medium underline" onClick={() => setDlError(null)}>Dismiss</button>
        </div>
      )}

      <div className="card p-6">
        {error ? (
          <div className="grid place-items-center min-h-[280px] text-center">
            <div>
              <p className="text-sm text-muted mb-3">Couldn't load the ledger.</p>
              <button className="btn btn-ghost !py-2 !px-4 text-sm" onClick={() => setReload((r) => r + 1)}>Try again</button>
            </div>
          </div>
        ) : loading ? (
          <ActivityListSkeleton />
        ) : entries.length === 0 ? (
          <div className="grid place-items-center min-h-[280px]">
            <p className="text-sm text-muted text-center max-w-[28ch]">No movements on this account yet.</p>
          </div>
        ) : (
          <>
            <p className="text-xs text-muted mb-2">{entries.length} movements · newest first</p>
            <ul className="divide-y divide-line max-h-none md:max-h-[62vh] overflow-y-auto md:pr-1">
              {entries.map((e) => <ActivityRow key={e.entryId} e={e} category={categories.get(e.entryId)} />)}
            </ul>
          </>
        )}
      </div>
    </div>
  )
}

function ActivityRow({ e, category }: { e: LedgerEntry; category?: string }) {
  const credit = e.type === 'CREDIT' || e.type === 'OPENING'
  const typeLabel = e.type === 'OPENING' ? 'Opening balance' : e.type === 'DEBIT' ? 'Transfer out' : e.type === 'CREDIT' ? 'Transfer in' : e.type
  const amt = parseFloat(e.signedAmount || '0')
  const date = new Date(e.createdAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
  const balance = parseFloat(e.balanceAfter || '0')
  return (
    <li className="py-3.5 flex items-center justify-between gap-4">
      <div className="flex items-center gap-3 min-w-0">
        <span aria-hidden="true" className={`w-9 h-9 rounded-full grid place-items-center shrink-0 ${credit ? 'bg-[rgba(12,166,120,.12)] text-pos' : 'bg-[rgba(229,72,77,.12)] text-neg'}`}>{credit ? '↑' : '↓'}</span>
        <div className="min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="text-sm font-medium">{e.description || typeLabel}</span>
            {category && (
              <span className="text-[10px] font-medium px-2 py-0.5 rounded-full" style={{ color: categoryColor(category), background: categoryColor(category) + '1f' }}>
                {label(category)}
              </span>
            )}
          </div>
          <div className="text-xs text-muted">{date} · {e.paymentId ? `ref ${e.paymentId}` : 'ledger'}</div>
        </div>
      </div>
      <div className="text-right shrink-0">
        <div className={`font-display ${credit ? 'text-pos' : 'text-neg'}`}>{money(amt)}</div>
        <div className="text-xs text-muted font-mono">bal {money(balance)}</div>
      </div>
    </li>
  )
}

function ActivityListSkeleton() {
  return (
    <div className="space-y-3">
      {[0, 1, 2, 3, 4].map((i) => (
        <div key={i} className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-full shimmer" />
          <div className="flex-1 h-3 rounded shimmer" />
          <div className="w-16 h-3 rounded shimmer" />
        </div>
      ))}
    </div>
  )
}