import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, ReferenceLine } from 'recharts'
import type { LedgerEntry } from '../types'
import { CHART } from '../lib/chartColors'

export const SpendingTrendChart = ({ data }: { data: LedgerEntry[] }) => {
  const byMonth: Record<number, { month: string; net: number }> = {}
  for (const e of data) {
    const d = new Date(e.createdAt)
    const key = d.getFullYear() * 12 + d.getMonth()
    if (!byMonth[key]) byMonth[key] = { month: d.toLocaleDateString('en-US', { month: 'short', year: '2-digit' }), net: 0 }
    byMonth[key].net += parseFloat(e.signedAmount || '0')
  }
  const chartData = Object.keys(byMonth)
    .map((k) => ({ key: Number(k), ...byMonth[Number(k)] }))
    .sort((a, b) => a.key - b.key)

  return (
    <div className="card p-6 flex flex-col">
      <h3 className="text-xl mb-1">Net cash flow</h3>
      <p className="text-sm text-muted mb-4">Income minus expenses, per month</p>
      {chartData.length === 0 ? (
        <div className="flex-1 grid place-items-center min-h-[240px]">
          <p className="text-sm text-muted">No activity yet.</p>
        </div>
      ) : (
        <div className="flex-1 min-h-[240px]" role="img" aria-label={`Net cash flow per month across ${chartData.length} months`}>
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
              <defs>
                <linearGradient id="netFlow" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor={CHART.accent} stopOpacity={0.18} />
                  <stop offset="100%" stopColor={CHART.accent} stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke={CHART.grid} />
              <XAxis dataKey="month" axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: CHART.tick }} dy={10} />
              <YAxis axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: CHART.tick }} dx={-10} tickFormatter={(v: number) => `$${v}`} />
              <Tooltip
                formatter={(v: any) => [`$${Number(v).toLocaleString('en-US', { maximumFractionDigits: 2 })}`, 'Net']}
                contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 20px rgba(0,0,0,0.08)' }}
              />
              <ReferenceLine y={0} stroke={CHART.grid} />
              <Area
                type="monotone"
                dataKey="net"
                name="Net"
                stroke={CHART.accent}
                strokeWidth={2}
                fill="url(#netFlow)"
                dot={{ r: 3, fill: CHART.accent, strokeWidth: 0 }}
                activeDot={{ r: 5 }}
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  )
}