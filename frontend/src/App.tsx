import { useEffect, useState } from 'react'
import Landing from './Landing'
import Login from './components/Login'
import Dashboard from './components/Dashboard'
import Transfer from './components/Transfer'
import AgentChat from './components/AgentChat'
import { Brand } from './components/Brand'
import { getAccounts } from './api'
import type { AccountView } from './types'

type View = 'dashboard' | 'transfer' | 'agent'

export default function App() {
  const [stage, setStage] = useState<'landing' | 'app'>('landing')
  const [token, setToken] = useState<string | null>(localStorage.getItem('jwt'))
  const [accounts, setAccounts] = useState<AccountView[]>([])
  const [view, setView] = useState<View>('dashboard')

  useEffect(() => {
    if (!token || stage !== 'app') return
    getAccounts().then((r: any) => setAccounts(r.data ?? [])).catch(() => setAccounts([]))
  }, [token, stage])

  function logout() {
    localStorage.removeItem('jwt')
    setToken(null)
    setAccounts([])
    setStage('landing')
  }

  if (stage === 'landing') return <Landing onEnter={() => setStage('app')} />
  if (!token) return <Login onLogin={(t) => { localStorage.setItem('jwt', t); setToken(t) }} />

  return (
    <div className="min-h-screen bg-bg text-ink">
      <header className="sticky top-0 z-30 px-4 md:px-8 py-4">
        <div className="glass rounded-full px-5 py-3 flex items-center justify-between shadow-soft">
          <div className="flex items-center gap-5">
            <button onClick={() => setView('dashboard')} className="flex items-center">
              <Brand tone="light" />
            </button>
            <nav className="hidden md:flex items-center gap-1 bg-[#EDEBE3] rounded-full p-1">
              {(['dashboard', 'transfer', 'agent'] as View[]).map((v) => (
                <button key={v} onClick={() => setView(v)}
                  className={`capitalize px-4 py-1.5 rounded-full text-sm transition ${view === v ? 'bg-accent text-white shadow-soft' : 'text-muted hover:text-ink'}`}>
                  {v}
                </button>
              ))}
            </nav>
          </div>
          <div className="flex items-center gap-3">
            <span className="hidden sm:flex items-center gap-2 text-sm text-muted">
              <span className="w-8 h-8 rounded-full bg-accent text-white grid place-items-center text-xs font-semibold">D</span>
              demo@bank.dev
            </span>
            <button onClick={logout} className="btn btn-ghost !py-2 !px-4 text-sm">Sign out</button>
          </div>
        </div>
        <nav className="md:hidden mt-3 glass rounded-full p-1 flex justify-between">
          {(['dashboard', 'transfer', 'agent'] as View[]).map((v) => (
            <button key={v} onClick={() => setView(v)}
              className={`flex-1 capitalize py-2 rounded-full text-sm transition ${view === v ? 'bg-accent text-white shadow-soft' : 'text-muted'}`}>{v}</button>
          ))}
        </nav>
      </header>

      <main className="px-4 md:px-8 pb-16 max-w-6xl mx-auto pt-6">
        <div key={view} className="view-in">
          {view === 'dashboard' && <Dashboard accounts={accounts} onTransfer={() => setView('transfer')} />}
          {view === 'transfer' && <Transfer accounts={accounts} onDone={() => { getAccounts().then((r: any) => setAccounts(r.data ?? [])).catch(() => {}); setView('dashboard') }} />}
          {view === 'agent' && <AgentChat />}
        </div>
      </main>
    </div>
  )
}