import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import type { LedgerEntry } from '../types'

export const SpendingTrendChart = ({ data }: { data: LedgerEntry[] }) => {
  const byMonth: Record<string, { month: string; income: number; expense: number }> = {}
  for (const e of data) {
    const month = new Date(e.createdAt).toLocaleDateString('en-US', { month: 'short', year: '2-digit' })
    if (!byMonth[month]) byMonth[month] = { month, income: 0, expense: 0 }
    const amt = parseFloat(e.signedAmount || '0')
    if (amt > 0) byMonth[month].income += amt
    else byMonth[month].expense += Math.abs(amt)
  }
  const chartData = Object.values(byMonth).sort((a, b) => a.month.localeCompare(b.month))

  if (chartData.length === 0)
    return (
      <div className="card p-6 flex flex-col">
        <h3 className="text-xl mb-1">Monthly cash flow</h3>
        <p className="text-sm text-muted">No activity yet.</p>
      </div>
    )

  return (
    <div className="card p-6 flex flex-col">
      <h3 className="text-xl mb-1">Monthly cash flow</h3>
      <p className="text-sm text-muted mb-4">Income vs expenses</p>
      <div className="flex-1 min-h-[300px]">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={chartData} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E5E7EB" />
            <XAxis dataKey="month" axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: '#6B7280' }} dy={10} />
            <YAxis axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: '#6B7280' }} dx={-10} tickFormatter={(v) => `$${v}`} />
            <Tooltip cursor={{ fill: 'rgba(0,0,0,0.04)' }} contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 20px rgba(0,0,0,0.08)' }} formatter={(v: any) => `$${v}`} />
            <Bar dataKey="income" name="Income" fill="#0CA678" radius={[4, 4, 0, 0]} maxBarSize={36} />
            <Bar dataKey="expense" name="Expense" fill="#E5484D" radius={[4, 4, 0, 0]} maxBarSize={36} />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}
