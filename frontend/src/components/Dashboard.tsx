import { useEffect, useRef, useState, useMemo } from "react"
import { downloadStatementCsv, downloadStatementExcel, classifySpending } from "../api"
import type { AccountView, CategorySpend, LedgerEntry } from "../types"
import { useCountUp } from "../lib/useCountUp"
import { formatMoney } from "../utils"
import { CategoryChart } from "./CategoryChart"
import { SpendingTrendChart } from "./SpendingTrendChart"
import { Mascot, type Mood } from "./mascot/Mascot"
import { useAllLedgers } from "../lib/queries"
import { useQuery } from "@tanstack/react-query"

export default function Dashboard({ accounts, onTransfer, onViewAll }: { accounts: AccountView[]; onTransfer: () => void; onViewAll: () => void }) {
  const [dlError, setDlError] = useState<string | null>(null)
  const [mood, setMood] = useState<Mood>("wave")
  const moodTimeoutRef = useRef<number | null>(null)

  const { data: rawLedgers, isLoading: ledgersLoading } = useAllLedgers(accounts, !!accounts.length)
  const allActivity = useMemo(() => {
    const all = [...(rawLedgers ?? [])].sort((a: LedgerEntry, b: LedgerEntry) => (b.createdAt ?? 0) - (a.createdAt ?? 0))
    return all
  }, [rawLedgers])
  const activity = useMemo(() => allActivity.slice(0, 6), [allActivity])
  const loading = ledgersLoading && allActivity.length === 0

  const classifyKey = allActivity.length > 0 ? allActivity.map((e) => e.entryId).join(",").slice(0, 200) : ""
  const { data: categorySummary = null, isError: classifyError, refetch: refetchClassify } = useQuery<CategorySpend[] | null>({
    queryKey: ["classify", classifyKey],
    queryFn: () => classifySpending(allActivity).catch(() => null),
    enabled: allActivity.length > 0,
    staleTime: 60_000,
  })

  useEffect(() => {
    const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches
    if (reduce) return
    let cancelled = false
    const toIdle = window.setTimeout(() => { if (!cancelled) setMood("idle") }, 2200)
    const schedule = () => {
      const delay = 2500 + Math.random() * 1500
      moodTimeoutRef.current = window.setTimeout(() => {
        if (cancelled) return
        setMood("wave")
        window.setTimeout(() => { if (!cancelled) setMood("idle") }, 2000)
        schedule()
      }, delay)
    }
    const initial = window.setTimeout(() => {
      if (cancelled) return
      setMood("wave")
      window.setTimeout(() => { if (!cancelled) setMood("idle") }, 2600)
      schedule()
    }, 500)
    return () => { cancelled = true; window.clearTimeout(toIdle); window.clearTimeout(initial); if (moodTimeoutRef.current) window.clearTimeout(moodTimeoutRef.current) }
  }, [])

  return (
    <div>
      <div className="flex items-end justify-between mb-8 flex-wrap gap-4">
        <div className="flex items-end gap-4">
          <Mascot size={110} mood={mood} className="shrink-0 cursor-pointer hover:scale-[1.03] transition-transform drop-shadow-sm" title="Click Sage to wave! 👋" onClick={() => { setMood("wave"); setTimeout(() => setMood("idle"), 2200); }} />
          <div>
            <p className="label">Good day, demo</p>
            <h1 className="text-4xl mt-1">Your accounts</h1>
            <p className="text-sm text-muted mt-1">Sage is watching your money.</p>
          </div>
        </div>
        <button onClick={onTransfer} className="btn btn-accent">New transfer</button>
      </div>

      <div className="grid md:grid-cols-2 gap-5">
        {accounts.map((a, i) => <AccountCard key={a.accountId} account={a} index={i} onDownloadError={setDlError} />)}
      </div>

      <div className="grid lg:grid-cols-2 gap-6 mt-6">
        <CategoryChart
          summary={categorySummary}
          error={!!classifyError}
          onRetry={() => refetchClassify()}
        />
        <SpendingTrendChart data={allActivity} loading={loading} />
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
  const num = useCountUp(parseFloat(account.balance || "0"))
  const run = (fn: () => Promise<void>, label: string) => {
    onDownloadError(null)
    fn().catch(() => onDownloadError(`Couldn\u0027t download ${label}. Try again.`))
  }
  return (
    <div className="card p-6 view-in" style={{ animationDelay: `${index * 80}ms` }}>
      <div className="flex items-center justify-between">
        <span className="chip capitalize">{account.type.toLowerCase()}</span>
        <span className="text-xs text-muted font-mono">{account.accountId}</span>
      </div>
      <div className="mt-5 font-display text-4xl">{formatMoney(num)}</div>
      <div className="mt-6 flex items-center justify-end gap-2">
        <button
          onClick={() => run(() => downloadStatementCsv(account.accountId), "CSV")}
          className="text-xs font-medium text-[#8A6D1A] bg-gold/15 hover:bg-gold/25 px-3 py-1.5 rounded-full transition-colors"
          title="Download statement as plain CSV"
        >
          CSV
        </button>
        <button
          onClick={() => run(() => downloadStatementExcel(account.accountId), "Excel")}
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
  const credit = e.type === "CREDIT" || e.type === "OPENING"
  const label = e.type === "OPENING" ? "Opening balance" : e.type === "DEBIT" ? "Transfer out" : e.type === "CREDIT" ? "Transfer in" : e.type
  const amt = parseFloat(e.signedAmount || "0")
  const formatted = (credit ? "+" : "−") + formatMoney(amt)
  const date = new Date(e.createdAt).toLocaleDateString("en-US", { month: "short", day: "numeric" })
  return (
    <li className="py-3.5 flex items-center justify-between">
      <div className="flex items-center gap-3">
        <span aria-hidden="true" className={`w-9 h-9 rounded-full grid place-items-center ${credit ? "bg-[rgba(11,138,99,.12)] text-pos" : "bg-[rgba(201,50,60,.12)] text-neg"}`}>{credit ? "↑" : "↓"}</span>
        <div>
          <div className="text-sm font-medium">{label}</div>
          <div className="text-xs text-muted">{date} · {e.paymentId ? `ref ${e.paymentId}` : "ledger"}</div>
        </div>
      </div>
      <div className={`font-display ${credit ? "text-pos" : "text-neg"}`}>{formatted}</div>
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
