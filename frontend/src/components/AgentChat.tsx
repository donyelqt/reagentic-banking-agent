import { useState } from 'react'
import { agentChat } from '../api'
import type { AgentResponse } from '../types'
import ApprovalModal from './ApprovalModal'

interface Msg {
  role: 'user' | 'agent'
  text: string
}

export default function AgentChat() {
  const [messages, setMessages] = useState<Msg[]>([])
  const [input, setInput] = useState('')
  const [last, setLast] = useState<AgentResponse | null>(null)
  const [busy, setBusy] = useState(false)

  async function send(text: string, body?: any) {
    setBusy(true)
    try {
      const res = await agentChat(body ?? { message: text })
      setLast(res)
      setMessages((m) => [...m, { role: 'agent', text: res.reply }])
      return res
    } finally {
      setBusy(false)
    }
  }

  function onSend(e: React.FormEvent) {
    e.preventDefault()
    if (!input.trim()) return
    const text = input.trim()
    setMessages((m) => [...m, { role: 'user', text }])
    setInput('')
    send(text)
  }

  function onApprove() {
    if (!last) return
    const stepIds = last.pendingSteps.map((s) => s.stepId)
    setMessages((m) => [...m, { role: 'user', text: 'Approved: ' + stepIds.join(', ') }])
    send('', { plan: last.plan, approval: stepIds })
  }

  return (
    <div className="flex flex-col h-[70vh] border rounded-xl bg-white">
      <div className="flex-1 overflow-auto p-4 space-y-2">
        {messages.length === 0 && (
          <p className="text-slate-400">
            Ask me to transfer money (I&apos;ll ask for approval) or reconcile an account.
          </p>
        )}
        {messages.map((m, i) => (
          <div key={i} className={m.role === 'user' ? 'text-right' : 'text-left'}>
            <span
              className={
                m.role === 'user'
                  ? 'inline-block bg-indigo-600 text-white rounded p-2'
                  : 'inline-block bg-slate-200 rounded p-2'
              }
            >
              {m.text}
            </span>
          </div>
        ))}
      </div>
      {last && last.pendingSteps.length > 0 && (
        <ApprovalModal steps={last.pendingSteps} onApprove={onApprove} onCancel={() => setLast(null)} />
      )}
      <form onSubmit={onSend} className="flex border-t p-2 gap-2">
        <input
          className="flex-1 border rounded p-2"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="e.g. transfer 50 to savings"
        />
        <button className="bg-indigo-600 text-white rounded px-4" disabled={busy}>
          {busy ? '…' : 'Send'}
        </button>
      </form>
    </div>
  )
}
