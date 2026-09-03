import { useState, useEffect } from "react"
import { downloadStatementCsv, downloadStatementExcel, classifyEntries } from "../api"
import type { AccountView, LedgerEntry } from "../types"
import { CATEGORY_COLORS } from "../lib/chartColors"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "./ui/select"
import { useLedger } from "../lib/queries"
import { useQuery } from "@tanstack/react-query"

const PAGE = 50

function money(v: number, sign = false): string {
  const abs = Math.abs(v).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  return (sign ? (v >= 0 ? "+" : "−") : v < 0 ? "−" : "") + "$" + abs
}

function label(category: string): string {
  return category.charAt(0).toUpperCase() + category.slice(1).toLowerCase()
}

function categoryColor(category: string): string {
  const names = ["Groceries", "Dining", "Transport", "Utilities", "Subscriptions", "Shopping", "Entertainment", "Health", "Travel", "Income", "Transfer", "Other"]
  const idx = names.indexOf(label(category))
  return CATEGORY_COLORS[(idx >= 0 ? idx : 11) % CATEGORY_COLORS.length]
}

export default function ActivityPage({ accounts }: { accounts: AccountView[] }) {
  const [accountId, setAccountId] = useState(accounts[0]?.accountId ?? "")
  const [visible, setVisible] = useState(PAGE)
  const [dlError, setDlError] = useState<string | null>(null)

  useEffect(() => { setVisible(PAGE) }, [accountId])

  const { data: entries = [], isLoading: loading, isError: error, refetch } = useLedger(accountId, !!accountId)

  const { data: categories = new Map<number, string>() } = useQuery({
    queryKey: ["classifyEntries", accountId, entries.length],
    queryFn: () => classifyEntries(entries),
    enabled: entries.length > 0,
    staleTime: 60_000,
  })

  const rows = entries.slice(0, visible)
  const run = (fn: () => Promise<void>, labelName: string) => {
    setDlError(null)
    fn().catch(() => setDlError(`Couldn\u0027t download ${labelName}. Try again.`))
  }

  return (
    <div>
      <div className="sticky top-14 md:top-0 z-20 -mx-4 md:-mx-8 px-4 md:px-8 pt-4 md:pt-6 pb-4 bg-bg border-b border-line">
        <div className="flex items-end justify-between flex-wrap gap-4">
          <div>
            <p className="label">Full history, straight from the ledger</p>
            <h1 className="text-4xl mt-1">Activity</h1>
          </div>
          <div className="flex items-center gap-3 flex-wrap">
            {accounts.length > 1 && (
              <label className="flex items-center gap-3">
                <span className="label">Account</span>
                <Select value={accountId} onValueChange={setAccountId}>
                  <SelectTrigger className="!w-auto" aria-label="Account"><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {accounts.map((a) => <SelectItem key={a.accountId} value={a.accountId}>{a.type} ({a.accountId})</SelectItem>)}
                  </SelectContent>
                </Select>
              </label>
            )}
            <div className="flex items-center gap-2">
              <button
                onClick={() => run(() => downloadStatementCsv(accountId), "CSV")}
                className="text-xs font-medium text-[#8A6D1A] bg-gold/15 hover:bg-gold/25 px-3 py-1.5 rounded-full transition-colors"
                title="Download statement as plain CSV"
              >
                CSV
              </button>
              <button
                onClick={() => run(() => downloadStatementExcel(accountId), "Excel")}
                className="text-xs font-medium text-accent bg-accent/10 hover:bg-accent/20 px-3 py-1.5 rounded-full transition-colors"
                title="Download the same statement as a styled Excel workbook"
              >
                Excel
              </button>
            </div>
          </div>
        </div>
      </div>

      {dlError && (
        <div className="tag-neg rounded-xl px-4 py-2.5 flex items-center justify-between text-sm my-4">
          <span>{dlError}</span>
          <button className="font-medium underline" onClick={() => setDlError(null)}>Dismiss</button>
        </div>
      )}

      <div className="card p-6 mt-4">
        {error ? (
          <div className="grid place-items-center min-h-[280px] text-center">
            <div>
              <p className="text-sm text-muted mb-3">Couldn\u0027t load the ledger.</p>
              <button className="btn btn-ghost !py-2 !px-4 text-sm" onClick={() => refetch()}>Try again</button>
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
            <ul className="divide-y divide-line">
              {rows.map((e) => <ActivityRow key={e.entryId} e={e} category={categories.get(e.entryId)} />)}
            </ul>
            {visible < entries.length && (
              <div className="flex items-center justify-center gap-4 mt-5">
                <button onClick={() => setVisible((v) => v + PAGE)} className="btn btn-ghost !py-2 !px-5 text-sm">
                  Show more
                </button>
                <span className="text-xs text-muted">Showing {rows.length} of {entries.length}</span>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}

function ActivityRow({ e, category }: { e: LedgerEntry; category?: string }) {
  const credit = e.type === "CREDIT" || e.type === "OPENING"
  const typeLabel = e.type === "OPENING" ? "Opening balance" : e.type === "DEBIT" ? "Transfer out" : e.type === "CREDIT" ? "Transfer in" : e.type
  const amt = parseFloat(e.signedAmount || "0")
  const date = new Date(e.createdAt).toLocaleDateString("en-US", { month: "short", day: "numeric" })
  const balance = parseFloat(e.balanceAfter || "0")
  return (
    <li className="py-3.5 flex items-center justify-between gap-4">
      <div className="flex items-center gap-3 min-w-0">
        <span aria-hidden="true" className={`w-9 h-9 rounded-full grid place-items-center shrink-0 ${credit ? "bg-[rgba(12,166,120,.12)] text-pos" : "bg-[rgba(229,72,77,.12)] text-neg"}`}>{credit ? "↑" : "↓"}</span>
        <div className="min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="text-sm font-medium">{e.description || typeLabel}</span>
            {category && (
              <span className="text-[10px] font-medium px-2 py-0.5 rounded-full" style={{ color: categoryColor(category), background: categoryColor(category) + "1f" }}>
                {label(category)}
              </span>
            )}
          </div>
          <div className="text-xs text-muted">{date} · {e.paymentId ? `ref ${e.paymentId}` : "ledger"}</div>
        </div>
      </div>
      <div className="text-right shrink-0">
        <div className={`font-display ${credit ? "text-pos" : "text-neg"}`}>{money(amt)}</div>
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
