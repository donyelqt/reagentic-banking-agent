import { useState, useRef, useEffect } from 'react'
import { agentChat } from '../api'
import type { AgentResponse } from '../types'
import ApprovalModal from './ApprovalModal'
import { isCapabilityQuestion, capabilityReply, introChips, actionChips, followUpChips } from '../lib/chatPrompts'

const GREETING = "Hello! I'm your banking agent. I can check balances, show your transactions, and move money between your accounts - transfers always wait for your approval."

interface Msg { role: 'user' | 'agent'; text: string; chips?: string[] }

export default function FloatingChat({ onExpand }: { onExpand: () => void }) {
  const [isOpen, setIsOpen] = useState(false)
  const [messages, setMessages] = useState<Msg[]>([
    { role: 'agent', text: GREETING }
  ])
  const [input, setInput] = useState('')
  const [last, setLast] = useState<AgentResponse | null>(null)
  const [busy, setBusy] = useState(false)
  const scrollRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (scrollRef.current) scrollRef.current.scrollTop = scrollRef.current.scrollHeight
  }, [messages, busy, isOpen])

  async function send(text: string, body?: any, chips?: string[]) {
    setBusy(true)
    try {
      const json: any = await agentChat(body ?? { message: text })
      const data: AgentResponse = json?.data ?? json
      setLast(data)
      const reply = data?.reply?.trim()
      if (reply) setMessages((m) => [...m, { role: 'agent', text: reply, chips }])
      return data
    } finally {
      setBusy(false)
    }
  }

  function handlePrompt(text: string) {
    const t = text.trim()
    if (!t) return
    setMessages((m) => [...m, { role: 'user', text: t }])
    setInput('')
    if (isCapabilityQuestion(t)) {
      setMessages((m) => [...m, { role: 'agent', text: capabilityReply(false), chips: actionChips(false) }])
      return
    }
    send(t, { message: t }, followUpChips(false, t))
  }

  function onSend(e: React.FormEvent) {
    e.preventDefault()
    handlePrompt(input)
  }

  function onApprove() {
    if (!last) return
    const ids = last.pendingSteps.map((s) => s.stepId)
    setMessages((m) => [...m, { role: 'user', text: 'Approved: ' + ids.join(', ') }])
    send('', { plan: last.plan, approval: ids }, followUpChips(false, 'Approved: ' + ids.join(', ')))
  }

  return (
    <div className="fixed bottom-6 right-6 z-40 flex flex-col items-end">
      {isOpen && (
        <div className="mb-4 w-[360px] max-w-[calc(100vw-48px)] h-[500px] max-h-[70vh] glass relative rounded-[26px] shadow-card flex flex-col overflow-hidden view-in">
          <div className="p-4 border-b border-line bg-surface/60 flex items-center justify-between">
            <h3 className="font-medium text-ink flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-pos animate-pulse" /> Banking Agent
            </h3>
            <div className="flex items-center gap-1">
              <button onClick={() => { setIsOpen(false); onExpand(); }} className="p-1.5 hover:bg-ink/10 rounded-lg text-muted hover:text-ink transition" title="Expand to full view">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M15 3h6v6M9 21H3v-6M21 3l-7 7M3 21l7-7"/></svg>
              </button>
              <button onClick={() => setIsOpen(false)} className="p-1.5 hover:bg-ink/10 rounded-lg text-muted hover:text-ink transition" aria-label="Close chat">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
              </button>
            </div>
          </div>
          
          <div ref={scrollRef} className="flex-1 overflow-y-auto p-5 space-y-4">
            {messages.map((m, i) => (
              <div key={i} className={`flex ${m.role === 'user' ? 'justify-end' : 'justify-start'} bubble-in`}>
                {m.role === 'user' ? (
                  <div className="max-w-[80%] rounded-2xl px-4 py-2.5 text-sm leading-relaxed whitespace-pre-line bg-[#2D43F5] text-white shadow-soft rounded-br-md">{m.text}</div>
                ) : (
                  <div className="max-w-[80%]">
                    <div className="rounded-2xl px-4 py-2.5 text-sm leading-relaxed whitespace-pre-line bg-white border border-line rounded-bl-md">{m.text}</div>
                    {m.chips && m.chips.length > 0 && (
                      <div className="mt-2.5 flex flex-wrap gap-2">
                        {m.chips.map((c) => (
                          <button key={c} onClick={() => handlePrompt(c)} disabled={busy}
                            className="text-xs px-3.5 py-1.5 rounded-full border border-accent/35 bg-white text-ink shadow-soft hover:bg-accent hover:text-white hover:border-accent hover:-translate-y-0.5 transition disabled:opacity-50">
                            {c}
                          </button>
                        ))}
                      </div>
                    )}
                  </div>
                )}
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
              <ApprovalModal steps={last.pendingSteps} onApprove={onApprove} onCancel={() => setLast(null)} busy={busy} />
           )}

          {messages.length === 1 && (
            <div className="px-5 pb-4 pt-2 flex justify-center">
              <button onClick={() => handlePrompt(introChips()[0])} disabled={busy}
                className="px-5 py-2 rounded-full border-2 border-dashed border-accent/50 text-accent font-medium text-sm shadow-soft hover:bg-accent hover:text-white hover:border-accent hover:border-solid hover:-translate-y-0.5 transition cursor-pointer disabled:opacity-50">
                {introChips()[0]}
              </button>
            </div>
          )}

          <form onSubmit={onSend} className="flex gap-2 p-3 border-t border-line bg-surface/60">
            <input className="field" value={input} onChange={(e) => setInput(e.target.value)} placeholder="Ask your agent..." />
            <button className="btn btn-accent px-5" disabled={busy}>{busy ? "..." : "Send"}</button>
          </form>
        </div>
      )}

      {!isOpen && (
        <button onClick={() => setIsOpen(true)} aria-label="Open banking agent chat" className="w-14 h-14 bg-[#2D43F5] text-white rounded-full shadow-[0_8px_30px_rgba(45,67,245,0.4)] flex items-center justify-center hover:scale-105 transition-transform">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
        </button>
      )}
    </div>
  )
}