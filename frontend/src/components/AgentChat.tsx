import { useState } from 'react'
import { agentChat } from '../api'
import type { AgentResponse } from '../types'
import ApprovalModal from './ApprovalModal'

interface Msg { role: 'user' | 'agent'; text: string }

// Initial greeting shown the moment the chat opens. Rendered client-side so the
// "hello" is always visible (the backend only returns this text as a *reply* to a
// message, so without this the empty state showed a generic placeholder instead).
const GREETING =
  "Hello! I'm your banking agent. I can help with: list accounts, balances, transactions, " +
  'transfers (with your approval), and reconciling an account. ' +
  'Try "transfer 50 to savings" or "reconcile my checking account".'

export default function AgentChat() {
  const [messages, setMessages] = useState<Msg[]>([{ role: 'agent', text: GREETING }])
  const [input, setInput] = useState('')
  const [last, setLast] = useState<AgentResponse | null>(null)
  const [busy, setBusy] = useState(false)

  async function send(text: string, body?: any) {
    setBusy(true)
    try {
      const json: any = await agentChat(body ?? { message: text })
      const data: AgentResponse = json?.data ?? json
      setLast(data)
      const reply = data?.reply?.trim()
      if (reply) setMessages((m) => [...m, { role: 'agent', text: reply }])
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
          {messages.map((m, i) => (
            <div key={i} className={`flex ${m.role === 'user' ? 'justify-end' : 'justify-start'} bubble-in`}>
              <div className={`max-w-[80%] rounded-2xl px-4 py-2.5 text-sm leading-relaxed ${m.role === 'user' ? 'bg-[#2D43F5] text-white shadow-soft rounded-br-md' : 'bg-white border border-line rounded-bl-md'}`}>{m.text}</div>
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