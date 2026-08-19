import { useEffect, useState } from 'react'
import { getInternalAccounts, getInternalLedger, reconcileAccount } from '../api'
import type { AccountView, LedgerEntry, ReconcileResult } from '../types'

const PAGE = 50

function money(v: number): string {
  const abs = Math.abs(v).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  return (v < 0 ? '−' : '') + '$' + abs
}

function typeLabel(type: string): string {
  return type === 'OPENING' ? 'Opening balance'
    : type === 'DEBIT' ? 'Transfer out'
    : type === 'CREDIT' ? 'Transfer in'
    : type
}

function typeChipClass(type: string): string {
  return type === 'OPENING' ? 'text-muted bg-line/60'
    : type === 'CREDIT' ? 'text-pos bg-[rgba(12,166,120,.12)]'
    : 'text-neg bg-[rgba(229,72,77,.12)]'
}

export default function LedgerConsole() {
  const [accounts, setAccounts] = useState<AccountView[]>([])
  const [accountsReady, setAccountsReady] = useState(false)
  const [accountId, setAccountId] = useState('')
  const [entries, setEntries] = useState<LedgerEntry[]>([])
  const [visible, setVisible] = useState(PAGE)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [reload, setReload] = useState(0)
  const [recon, setRecon] = useState<ReconcileResult | null>(null)
  const [reconciling, setReconciling] = useState(false)
  const [reconError, setReconError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    getInternalAccounts()
      .then((r: any) => {
        if (cancelled) return
        const list: AccountView[] = r.data ?? []
        setAccounts(list)
        setAccountId((prev) => (list.some((a) => a.accountId === prev) ? prev : (list[0]?.accountId ?? '')))
      })
      .catch(() => { if (!cancelled) setAccounts([]) })
      .finally(() => { if (!cancelled) setAccountsReady(true) })
    return () => { cancelled = true }
  }, [])

  useEffect(() => {
    if (!accountId) return
    let cancelled = false
    setLoading(true)
    setError(false)
    setVisible(PAGE)
    setRecon(null)
    setReconError(null)
    getInternalLedger(accountId)
      .then((r: any) => {
        if (!cancelled) setEntries(r.data ?? [])
      })
      .catch(() => { if (!cancelled) setError(true) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [accountId, reload])

  const runReconcile = async () => {
    if (reconciling) return
    setReconciling(true)
    setReconError(null)
    try {
      const r: any = await reconcileAccount(accountId)
      setRecon(r?.data ?? r)
    } catch (err: any) {
      let msg = "Reconciliation failed. Try again."
      try {
        const parsed = JSON.parse(err?.message ?? "")
        if (parsed?.message) msg = parsed.message
      } catch { /* keep default */ }
      setReconError(msg)
    } finally {
      setReconciling(false)
    }
  }

  const rows = entries.slice(0, visible)
  const account = accounts.find((a) => a.accountId === accountId)

  return (
    <div>
      <div className="flex items-end justify-between flex-wrap gap-4">
        <div>
          <p className="label">Internal ops · manual</p>
          <h1 className="text-4xl mt-1">Ledger Console</h1>
          <p className="text-sm text-muted mt-2 max-w-[52ch]">
            The ops agent's powers, hands-on: inspect any account, run a reconciliation, browse the immutable ledger.
          </p>
        </div>
        <label className="flex items-center gap-3">
          <span className="label">Account</span>
          <select className="field !w-auto" value={accountId} onChange={(e) => setAccountId(e.target.value)}>
            {accounts.map((a) => <option key={a.accountId} value={a.accountId}>{a.type} ({a.accountId})</option>)}
          </select>
        </label>
      </div>

      {!accountsReady ? (
        <div className="card p-6 mt-6 grid place-items-center min-h-[280px]">
          <div className="w-full max-w-sm space-y-3">
            <div className="h-3 rounded shimmer" />
            <div className="h-3 rounded shimmer" />
            <div className="h-10 rounded-2xl shimmer" />
          </div>
        </div>
      ) : accounts.length === 0 ? (
        <div className="card p-6 mt-6 grid place-items-center min-h-[280px]">
          <p className="text-sm text-muted text-center max-w-[34ch]">No accounts to inspect.</p>
        </div>
      ) : (
      <div className="mt-6 grid lg:grid-cols-3 gap-4">
        <div className="card p-6 flex flex-col justify-between gap-5">
          <div>
            <p className="label">Available balance</p>
            <p className="text-4xl font-display mt-2">{recon ? money(parseFloat(recon.balance)) : account ? money(parseFloat(account.balance)) : '—'}</p>
            <p className="text-xs text-muted mt-1 font-mono">{account?.accountId}</p>
          </div>
          <div className="flex items-center justify-between gap-3">
            <button onClick={runReconcile} disabled={reconciling || !accountId} className="btn btn-accent">
              {reconciling ? 'Reconciling…' : recon ? 'Reconcile again' : 'Reconcile'}
            </button>
            {recon && (
              recon.balanced
                ? <span className="tag-pos">Balanced</span>
                : <span className="tag-neg">Mismatch</span>
            )}
          </div>
        </div>

        <div className="card p-6 lg:col-span-2">
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-medium">Reconciliation</h2>
            <span className="text-xs text-muted">balance vs immutable ledger</span>
          </div>

          {reconError && (
            <div className="tag-neg rounded-xl px-4 py-2.5 text-sm mb-4">{reconError}</div>
          )}

          {!recon && !reconError && (
            <div className="grid place-items-center min-h-[180px] text-center">
              <p className="text-sm text-muted max-w-[34ch]">
                Run a reconciliation to compare the account balance against the ledger and get root-cause evidence if they break.
              </p>
            </div>
          )}

          {recon && recon.balanced && (
            <div className="space-y-3">
              <p className="text-sm text-pos font-medium">Account {recon.accountId} is balanced — ledger and balance agree.</p>
              <div className="flex flex-wrap gap-x-8 gap-y-2 text-sm">
                <span><span className="text-muted">balance</span> <span className="font-mono ml-1">{money(parseFloat(recon.balance))}</span></span>
                <span><span className="text-muted">ledger sum</span> <span className="font-mono ml-1">{money(parseFloat(recon.ledgerSum))}</span></span>
              </div>
            </div>
          )}

          {recon && !recon.balanced && (
            <div className="space-y-4">
              <div className="rounded-xl border border-line bg-bg px-4 py-3 text-sm space-y-2">
                <p className="text-neg font-medium">Mismatch on {recon.accountId}</p>
                <div className="flex flex-wrap gap-x-8 gap-y-2">
                  <span><span className="text-muted">balance</span> <span className="font-mono ml-1">{money(parseFloat(recon.balance))}</span></span>
                  <span><span className="text-muted">ledger sum</span> <span className="font-mono ml-1">{money(parseFloat(recon.ledgerSum))}</span></span>
                  <span><span className="text-muted">delta</span> <span className="font-mono ml-1">{money(parseFloat(recon.delta ?? '0'))}</span></span>
                  <span><span className="text-muted">missing</span> <span className="font-mono ml-1">{recon.direction === 'MISSING_DEBIT_LEG' ? 'Dr' : 'Cr'} {money(parseFloat(recon.missingAmount ?? '0'))}</span></span>
                </div>
                <p className="text-xs text-muted leading-relaxed">{recon.diagnosis}</p>
                <p className="text-xs text-muted font-mono">
                  ledger ends at balanceAfter={money(parseFloat(recon.lastBalanceAfter ?? '0'))} · entry #{recon.lastEntryId} · ref {recon.lastPaymentId}
                </p>
              </div>
              <div>
                <p className="text-xs text-muted mb-2">Evidence trail (last {recon.evidence?.length ?? 0} of {recon.evidenceCount} ledger entries)</p>
                <ul className="divide-y divide-line rounded-xl border border-line bg-bg px-4">
                  {recon.evidence?.map((e) => (
                    <li key={e.entryId} className="py-2 flex items-center justify-between gap-3 text-xs">
                      <span className="flex items-center gap-2 min-w-0">
                        <span className="text-muted font-mono shrink-0">#{e.entryId}</span>
                        <span className={`text-[10px] font-medium px-2 py-0.5 rounded-full shrink-0 ${typeChipClass(e.type)}`}>{typeLabel(e.type)}</span>
                        <span className="text-muted truncate font-mono">{e.paymentId === 'OPENING' ? 'ledger' : `ref ${e.paymentId}`}</span>
                      </span>
                      <span className="flex items-center gap-3 shrink-0">
                        <span className="font-mono">{money(parseFloat(e.signedAmount))}</span>
                        <span className="text-muted font-mono">bal {money(parseFloat(e.balanceAfter))}</span>
                      </span>
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          )}
        </div>
      </div>
      )}

      <div className="card p-6 mt-4">
        {error ? (
          <div className="grid place-items-center min-h-[280px] text-center">
            <div>
              <p className="text-sm text-muted mb-3">Couldn't load the ledger.</p>
              <button className="btn btn-ghost !py-2 !px-4 text-sm" onClick={() => setReload((r) => r + 1)}>Try again</button>
            </div>
          </div>
        ) : loading ? (
          <LedgerSkeleton />
        ) : entries.length === 0 ? (
          <div className="grid place-items-center min-h-[280px]">
            <p className="text-sm text-muted text-center max-w-[28ch]">No movements on this account yet.</p>
          </div>
        ) : (
          <>
            <ul className="divide-y divide-line">
              {rows.map((e) => <LedgerRow key={e.entryId} e={e} />)}
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

function LedgerRow({ e }: { e: LedgerEntry }) {
  const credit = e.type === 'CREDIT' || e.type === 'OPENING'
  const amt = parseFloat(e.signedAmount || '0')
  const date = new Date(e.createdAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
  return (
    <li className="py-3.5 flex items-center justify-between gap-4">
      <div className="flex items-center gap-3 min-w-0">
        <span aria-hidden="true" className={`w-9 h-9 rounded-full grid place-items-center shrink-0 ${credit ? 'bg-[rgba(12,166,120,.12)] text-pos' : 'bg-[rgba(229,72,77,.12)] text-neg'}`}>{credit ? '↑' : '↓'}</span>
        <div className="min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="text-sm font-medium">{e.description || typeLabel(e.type)}</span>
            <span className={`text-[10px] font-medium px-2 py-0.5 rounded-full ${typeChipClass(e.type)}`}>{typeLabel(e.type)}</span>
          </div>
          <div className="text-xs text-muted">{date} · {e.paymentId ? `ref ${e.paymentId}` : 'ledger'}</div>
        </div>
      </div>
      <div className="text-right shrink-0">
        <div className={`font-display ${credit ? 'text-pos' : 'text-neg'}`}>{money(amt)}</div>
        <div className="text-xs text-muted font-mono">bal {money(parseFloat(e.balanceAfter || '0'))}</div>
      </div>
    </li>
  )
}

function LedgerSkeleton() {
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