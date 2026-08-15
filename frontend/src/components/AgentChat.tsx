import { useState } from 'react'
import { agentChat } from '../api'
import type { AgentResponse } from '../types'
import ApprovalModal from './ApprovalModal'

interface Msg { role: 'user' | 'agent'; text: string }

export default function AgentChat() {
  const [messages, setMessages] = useState<Msg[]>([])
  const [input, setInput] = useState('')
  const [last, setLast] = useState<AgentResponse | null>(null)
  const [busy, setBusy] = useState(false)

  async function send(text: string, body?: any) {
    setBusy(true)
    try {
      const json: any = await agentChat(body ?? { message: text })
      const data: AgentResponse = json?.data ?? json
      setLast(data)
      setMessages((m) => [...m, { role: 'agent', text: data.reply }])
      return data
    } finally { setBusy(false) }
  }

  function onSend(e: React.FormEvent) {
    e.preventDefault()
    const t = input.trim()
    if (!t) return
    setMessages((m) => [...m, { role: 'user', text: t }])
    setInput('')
    send(t)
  }

  function onApprove() {
    if (!last) return
    const ids = last.pendingSteps.map((s) => s.stepId)
    setMessages((m) => [...m, { role: 'user', text: 'Approved: ' + ids.join(', ') }])
    send('', { plan: last.plan, approval: ids })
  }

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-4xl mb-1">Agent</h1>
      <p className="text-muted mb-6">Ask in plain language. Risky moves wait for your approval.</p>
      <div className="glass relative rounded-[26px] h-[64vh] flex flex-col overflow-hidden shadow-card">
        <div className="flex-1 overflow-auto p-5 space-y-4">
          {messages.length === 0 && (
            <div className="h-full grid place-items-center text-center">
              <div className="max-w-sm">
                <div className="w-14 h-14 rounded-2xl mx-auto mb-4 grid place-items-center" style={{ background: 'conic-gradient(from 200deg,#2D43F5,#6A4BFF,#0CA678,#2D43F5)' }}>
                  <span className="w-4 h-4 rounded-full bg-white" />
                </div>
                <p className="text-muted">Try: <span className="text-ink">“transfer 50 to savings”</span> or <span className="text-ink">“reconcile checking”</span></p>
              </div>
            </div>
          )}
          {messages.map((m, i) => (
            <div key={i} className={`flex ${m.role === 'user' ? 'justify-end' : 'justify-start'} bubble-in`}>
              <div className={`max-w-[80%] rounded-2xl px-4 py-2.5 text-sm leading-relaxed ${m.role === 'user' ? 'bg-accent text-white rounded-br-md' : 'bg-white border border-line rounded-bl-md'}`}>{m.text}</div>
            </div>
          ))}
          {busy && (
            <div className="flex justify-start bubble-in">
              <div className="bg-white border border-line rounded-2xl rounded-bl-md px-4 py-3">
                <span className="w-4 h-4 border-2 border-ink/20 border-t-accent rounded-full spin inline-block" />
              </div>
            </div>
          )}
        </div>
        {last && last.pendingSteps.length > 0 && (
          <ApprovalModal steps={last.pendingSteps} onApprove={onApprove} onCancel={() => setLast(null)} />
        )}
        <form onSubmit={onSend} className="flex gap-2 p-3 border-t border-line bg-surface/60">
          <input className="field" value={input} onChange={(e) => setInput(e.target.value)} placeholder="Ask the agent..." />
          <button className="btn btn-accent px-5" disabled={busy}>{busy ? '...' : 'Send'}</button>
        </form>
      </div>
    </div>
  )
}