import { useState } from 'react'
import { login } from '../api'
import { Brand } from './Brand'

export default function Login({ onLogin }: { onLogin: (token: string) => void }) {
  const [email, setEmail] = useState('demo@bank.dev')
  const [password, setPassword] = useState('demo1234')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setLoading(true); setError('')
    try {
      const token = await login(email, password)
      localStorage.setItem('jwt', token)
      onLogin(token)
    } catch (err: any) {
      setError(err.message || 'Login failed')
    } finally { setLoading(false) }
  }

  return (
    <div className="stage-light grain min-h-screen grid place-items-center px-4">
      <div className="orb orb-1" /><div className="orb orb-2" />
      <div className="relative z-10 w-full max-w-md">
        <div className="flex justify-center mb-8"><Brand tone="light" /></div>
        <div className="glass rounded-[28px] p-8 shadow-lift">
          <h1 className="text-3xl">Welcome back</h1>
          <p className="text-muted mt-2 text-sm">Sign in to your private agentic bank.</p>
          <form onSubmit={submit} className="mt-7 space-y-4">
            <div>
              <label className="label">Email</label>
              <input className="field mt-1.5" type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" />
            </div>
            <div>
              <label className="label">Password</label>
              <input className="field mt-1.5" type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" />
            </div>
            {error && <div className="text-neg text-sm bg-[rgba(229,72,77,.1)] rounded-xl px-3 py-2">{error}</div>}
            <button type="submit" disabled={loading} className="btn btn-accent w-full !py-3 text-base cta-pulse disabled:opacity-60">
              {loading ? <span className="w-5 h-5 border-2 border-white/40 border-t-white rounded-full spin" /> : 'Sign in'}
            </button>
          </form>
          <p className="text-center text-muted text-xs mt-5">Demo · demo@bank.dev / demo1234</p>
        </div>
      </div>
    </div>
  )
}