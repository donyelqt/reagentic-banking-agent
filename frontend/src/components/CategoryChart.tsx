import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts'
import type { CategorySpend } from '../types'

const COLORS = ['#2D43F5', '#0CA678', '#F59E0B', '#E5484D', '#6A4BFF', '#0EA5E9', '#C9A227', '#14130F']

function label(category: string): string {
  return category.charAt(0).toUpperCase() + category.slice(1).toLowerCase()
}

function money(v: number): string {
  return '$' + v.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export const CategoryChart = ({
  summary,
  error,
  onRetry
}: {
  summary: CategorySpend[] | null
  error: boolean
  onRetry: () => void
}) => {
  return (
    <div className="card p-6 flex flex-col">
      <h3 className="text-xl mb-1">Spending by category</h3>
      <p className="text-sm text-muted mb-4">Classified from your ledger</p>

      {error ? (
        <div className="flex-1 grid place-items-center min-h-[260px]">
          <div className="text-center">
            <p className="text-sm text-muted mb-3">Couldn't classify your spending.</p>
            <button onClick={onRetry} className="btn btn-ghost !py-2 !px-4 text-sm">Try again</button>
          </div>
        </div>
      ) : summary === null ? (
        <div className="flex-1 min-h-[260px] space-y-3" aria-busy="true" aria-label="Loading spending categories">
          <div className="w-40 h-40 rounded-full shimmer mx-auto" />
          {[0, 1, 2].map((i) => (
            <div key={i} className="flex items-center gap-3">
              <div className="w-8 h-3 rounded shimmer" />
              <div className="flex-1 h-3 rounded shimmer" />
            </div>
          ))}
        </div>
      ) : summary.length === 0 ? (
        <div className="flex-1 grid place-items-center min-h-[260px]">
          <p className="text-sm text-muted text-center max-w-[28ch]">
            No categorized spending yet. Move money and the agent will classify it.
          </p>
        </div>
      ) : (
        <>
          <div className="flex-1 min-h-[240px]">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={summary}
                  dataKey="total"
                  nameKey="category"
                  innerRadius={60}
                  outerRadius={90}
                  paddingAngle={3}
                >
                  {summary.map((_, i) => <Cell key={`cell-${i}`} fill={COLORS[i % COLORS.length]} />)}
                </Pie>
                <Tooltip
                  formatter={(v: any, name: any) => [money(v), label(String(name))]}
                  contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 20px rgba(0,0,0,0.08)' }}
                />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <ul className="mt-4 space-y-1.5">
            {summary.map((s, i) => (
              <li key={s.category} className="flex items-center justify-between text-sm">
                <span className="flex items-center gap-2 text-ink">
                  <span className="w-2.5 h-2.5 rounded-full" style={{ background: COLORS[i % COLORS.length] }} />
                  {label(s.category)}
                </span>
                <span className="font-medium font-mono">{money(s.total)}</span>
              </li>
            ))}
            <li className="flex items-center justify-between text-sm pt-2 mt-2 border-t border-line">
              <span className="text-muted">Total spend</span>
              <span className="font-semibold font-mono">{money(summary.reduce((a, s) => a + s.total, 0))}</span>
            </li>
          </ul>
        </>
      )}
    </div>
  )
}