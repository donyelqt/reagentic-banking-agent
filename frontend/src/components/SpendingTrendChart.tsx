import { BarChart, Bar, Cell, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, ReferenceLine } from 'recharts'
import type { LedgerEntry } from '../types'
import { CHART } from '../lib/chartColors'

function monthKey(d: Date) {
  return d.getFullYear() * 12 + d.getMonth()
}

function monthLabel(k: number) {
  return new Date(Math.floor(k / 12), k % 12, 1).toLocaleDateString('en-US', { month: 'short', year: '2-digit' })
}

function compactMoney(v: number) {
  const a = Math.abs(v)
  const s = a >= 1000 ? (a % 1000 === 0 ? `${a / 1000}k` : `${(a / 1000).toFixed(1)}k`) : String(a)
  return `${v < 0 ? '−' : ''}$${s}`
}

function money(v: number) {
  return `${v < 0 ? '−' : '+'}$${Math.abs(v).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

export const SpendingTrendChart = ({ data, loading }: { data: LedgerEntry[]; loading: boolean }) => {
  const currentKey = monthKey(new Date())

  const byMonth: Record<number, number> = {}
  for (const e of data) {
    if (e.type === 'OPENING') continue
    const k = monthKey(new Date(e.createdAt))
    byMonth[k] = (byMonth[k] ?? 0) + parseFloat(e.signedAmount || '0')
  }

  const keys = Object.keys(byMonth).map(Number)
  const chartData: { month: string; pos: number; neg: number; mtd: boolean }[] = []
  if (keys.length > 0) {
    const [min, max] = [Math.min(...keys), Math.max(...keys)]
    for (let k = min; k <= max; k++) {
      const net = byMonth[k] ?? 0
      chartData.push({ month: monthLabel(k), pos: net > 0 ? net : 0, neg: net < 0 ? net : 0, mtd: k === currentKey })
    }
  }

  const hasMtd = chartData.some((d) => d.mtd)
  const best = chartData.reduce((m, d) => Math.max(m, d.pos), 0)
  const worst = chartData.reduce((m, d) => Math.min(m, d.neg), 0)

  return (
    <div className="card p-6 flex flex-col">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h3 className="text-xl mb-1">Net cash flow</h3>
          <p className={`text-sm text-muted ${best > 0 || worst < 0 ? 'mb-1' : 'mb-4'}`}>Income minus expenses, per month</p>
          {(best > 0 || worst < 0) && (
            <p className="text-xs text-muted mb-4">Best month {money(best)} · Worst month {money(worst)}</p>
          )}
        </div>
        <div className="flex items-center gap-3 text-xs text-muted shrink-0 pt-1">
          <span className="flex items-center gap-1.5"><span className="w-2 h-2 rounded-full bg-pos" />Surplus</span>
          <span className="flex items-center gap-1.5"><span className="w-2 h-2 rounded-full bg-neg" />Deficit</span>
          {hasMtd && <span className="chip" title="Current month, shown month-to-date">MTD</span>}
        </div>
      </div>

      {loading ? (
        <div className="flex-1 min-h-[240px] space-y-3" aria-busy="true" aria-label="Loading net cash flow">
          {[0, 1, 2, 3].map((i) => (
            <div key={i} className="h-10 rounded shimmer" style={{ opacity: 1 - i * 0.22 }} />
          ))}
        </div>
      ) : chartData.length === 0 ? (
        <div className="flex-1 grid place-items-center min-h-[240px]">
          <p className="text-sm text-muted text-center max-w-[28ch]">
            No activity yet. Make a transfer and the cash flow chart will fill in.
          </p>
        </div>
      ) : (
        <>
          <div className="flex-1 min-h-[240px]" aria-hidden="true">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData} margin={{ top: 10, right: 10, left: -14, bottom: 0 }} barCategoryGap="28%">
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke={CHART.grid} />
                <XAxis dataKey="month" axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: CHART.tick }} dy={10} />
                <YAxis axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: CHART.tick }} dx={-6} tickFormatter={(v: number) => compactMoney(v)} />
                <Tooltip
                  formatter={(v: any) => (v === 0 || v == null ? null : [money(Number(v)), 'Net'])}
                  contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 20px rgba(0,0,0,0.08)' }}
                />
                <ReferenceLine y={0} stroke={CHART.grid} />
                <Bar dataKey="pos" name="Net" radius={[6, 6, 0, 0]} maxBarSize={30}>
                  {chartData.map((d, i) => <Cell key={`p-${i}`} fill={CHART.pos} fillOpacity={d.mtd ? 0.45 : 1} />)}
                </Bar>
                <Bar dataKey="neg" name="Net" radius={[0, 0, 6, 6]} maxBarSize={30}>
                  {chartData.map((d, i) => <Cell key={`n-${i}`} fill={CHART.neg} fillOpacity={d.mtd ? 0.45 : 1} />)}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
          <table className="sr-only">
            <caption>Net cash flow per month</caption>
            <thead>
              <tr><th scope="col">Month</th><th scope="col">Net</th></tr>
            </thead>
            <tbody>
              {chartData.map((d) => (
                <tr key={d.month}>
                  <th scope="row">{d.month}{d.mtd ? ' (month-to-date)' : ''}</th>
                  <td>{d.pos > 0 ? money(d.pos) : d.neg < 0 ? money(d.neg) : '$0.00'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}
    </div>
  )
}
