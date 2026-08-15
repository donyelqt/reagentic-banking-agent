import { useEffect, useState } from 'react'
import Login from './components/Login'
import Transfer from './components/Transfer'
import AgentChat from './components/AgentChat'
import { getAccounts } from './api'
import type { AccountView } from './types'

type View = 'dashboard' | 'transfer' | 'agent'

export default function App() {
  const [token, setToken] = useState<string | null>(localStorage.getItem('jwt'))
  const [accounts, setAccounts] = useState<AccountView[]>([])
  const [view, setView] = useState<View>('dashboard')

  useEffect(() => {
    if (!token) return
    getAccounts()
      .then((r) => setAccounts(r.data ?? []))
      .catch(() => setAccounts([]))
  }, [token])

  function logout() {
    localStorage.removeItem('jwt')
    setToken(null)
  }

  if (!token) return <Login onLogin={setToken} />

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="flex items-center justify-between px-6 py-3 bg-white shadow">
        <h1 className="font-bold text-indigo-700">Reagentic Bank</h1>
        <button className="text-sm text-slate-500" onClick={logout}>
          Sign out
        </button>
      </header>
      <nav className="flex gap-4 px-6 py-2 bg-white border-b">
        {(['dashboard', 'transfer', 'agent'] as View[]).map((v) => (
          <button
            key={v}
            onClick={() => setView(v)}
            className={view === v ? 'font-semibold text-indigo-700 capitalize' : 'text-slate-500 capitalize'}
          >
            {v}
          </button>
        ))}
      </nav>
      <main className="p-6">
        {view === 'dashboard' && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {accounts.map((a) => (
              <div key={a.accountId} className="bg-white rounded-xl shadow p-5">
                <p className="text-slate-500 capitalize">{a.type}</p>
                <p className="text-2xl font-bold">${a.balance}</p>
                <p className="text-xs text-slate-400">{a.accountId}</p>
              </div>
            ))}
            {accounts.length === 0 && <p className="text-slate-400">No accounts found.</p>}
          </div>
        )}
        {view === 'transfer' && <Transfer accounts={accounts} />}
        {view === 'agent' && <AgentChat />}
      </main>
    </div>
  )
}
