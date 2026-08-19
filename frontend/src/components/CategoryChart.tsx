import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend } from 'recharts'
import type { AccountView } from '../types'

const COLORS = ['#2D43F5', '#0CA678', '#F59E0B', '#E5484D', '#6A4BFF', '#0EA5E9']

export const CategoryChart = ({ accounts }: { accounts: AccountView[] }) => {
  const data = accounts.map((a) => ({
    name: a.type.charAt(0).toUpperCase() + a.type.slice(1).toLowerCase(),
    value: Math.max(0, parseFloat(a.balance || '0')),
  }))

  if (data.length === 0 || data.every((d) => d.value === 0))
    return (
      <div className="card p-6 flex flex-col">
        <h3 className="text-xl mb-1">Balance by account</h3>
        <p className="text-sm text-muted">No balances to display yet.</p>
      </div>
    )

  return (
    <div className="card p-6 flex flex-col">
      <h3 className="text-xl mb-1">Balance by account</h3>
      <p className="text-sm text-muted mb-4">Distribution of holdings</p>
      <div className="flex-1 min-h-[300px]">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie data={data} dataKey="value" nameKey="name" innerRadius={60} outerRadius={90} paddingAngle={3}>
              {data.map((_, i) => <Cell key={`cell-${i}`} fill={COLORS[i % COLORS.length]} />)}
            </Pie>
            <Tooltip formatter={(v: any) => `$${v}`} contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 20px rgba(0,0,0,0.08)' }} />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}
