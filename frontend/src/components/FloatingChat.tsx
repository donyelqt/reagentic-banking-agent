import { useState, useRef, useEffect } from 'react'
import { agentChat } from '../api'
import type { AgentResponse } from '../types'
import ApprovalModal from './ApprovalModal'

const GREETING = "Hello! I'm your banking agent. How can I assist you today?"

export default function FloatingChat({ onExpand }: { onExpand: () => void }) {
  const [isOpen, setIsOpen] = useState(false)
  const [messages, setMessages] = useState<{ role: 'user' | 'agent'; text: string }[]>([
    { role: 'agent', text: GREETING }
  ])
  const [input, setInput] = useState('')
  const [last, setLast] = useState<AgentResponse | null>(null)
  const [busy, setBusy] = useState(false)
  const scrollRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (scrollRef.current) scrollRef.current.scrollTop = scrollRef.current.scrollHeight
  }, [messages, busy, isOpen])

  async function send(text: string, body?: any) {
    setBusy(true)
    try {
      const json: any = await agentChat(body ?? { message: text })
      const data: AgentResponse = json?.data ?? json
      setLast(data)
      const reply = data?.reply?.trim()
      if (reply) setMessages((m) => [...m, { role: 'agent', text: reply }])
    } finally {
      setBusy(false)
    }
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
    <div className="fixed bottom-6 right-6 z-40 flex flex-col items-end">
      {isOpen && (
        <div className="mb-4 w-[360px] max-w-[calc(100vw-48px)] h-[500px] max-h-[70vh] !bg-[#13151A]/80 backdrop-blur-2xl border border-white/10 rounded-3xl shadow-[0_24px_50px_-12px_rgba(0,0,0,0.5),inset_0_1px_1px_rgba(255,255,255,0.15)] flex flex-col overflow-hidden view-in relative">
          <div className="p-4 border-b border-white/10 bg-black/20 flex items-center justify-between">
            <h3 className="font-medium text-white flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-pos animate-pulse" /> Banking Agent
            </h3>
            <div className="flex items-center gap-1">
              <button onClick={() => { setIsOpen(false); onExpand(); }} className="p-1.5 hover:bg-white/10 rounded-lg text-white/60 hover:text-white transition" title="Expand to full view">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M15 3h6v6M9 21H3v-6M21 3l-7 7M3 21l7-7"/></svg>
              </button>
              <button onClick={() => setIsOpen(false)} className="p-1.5 hover:bg-white/10 rounded-lg text-white/60 hover:text-white transition">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
              </button>
            </div>
          </div>
          
          <div ref={scrollRef} className="flex-1 overflow-y-auto p-4 space-y-4">
            {messages.map((m, i) => (
              <div key={i} className={`flex ${m.role === 'user' ? 'justify-end' : 'justify-start'} bubble-in`}>
                <div className={`max-w-[85%] rounded-2xl px-3.5 py-2 text-sm leading-relaxed ${m.role === 'user' ? 'bg-[#2D43F5] text-white shadow-soft rounded-br-md' : 'bg-black/40 border border-white/10 text-white/90 rounded-bl-md'}`}>
                  {m.text}
                </div>
              </div>
            ))}
            {busy && (
              <div className="flex justify-start bubble-in">
                <div className="bg-black/40 border border-white/10 rounded-2xl rounded-bl-md px-4 py-2">
                  <span className="w-3.5 h-3.5 border-2 border-white/20 border-t-accent rounded-full spin inline-block" />
                </div>
              </div>
            )}
          </div>
          
          {last && last.pendingSteps.length > 0 && (
             <ApprovalModal steps={last.pendingSteps} onApprove={onApprove} onCancel={() => setLast(null)} />
          )}

          <form onSubmit={onSend} className="p-3 border-t border-white/10 bg-black/20 flex gap-2">
            <input className="field py-2 text-sm !bg-black/30 !border-white/10 !text-white placeholder:text-white/40 focus:!border-white/30" value={input} onChange={(e) => setInput(e.target.value)} placeholder="Type a message..." />
            <button className="btn btn-accent px-4 py-2" disabled={busy}>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z"/></svg>
            </button>
          </form>
        </div>
      )}

      {!isOpen && (
        <button onClick={() => setIsOpen(true)} className="w-14 h-14 bg-[#2D43F5] text-white rounded-full shadow-[0_8px_30px_rgba(45,67,245,0.4)] flex items-center justify-center hover:scale-105 transition-transform">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
        </button>
      )}
    </div>
  )
}
