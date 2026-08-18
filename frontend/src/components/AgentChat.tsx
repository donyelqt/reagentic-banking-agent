import { useState } from "react";
import { agentChat } from "../api";
import type { AgentResponse } from "../types";
import ApprovalModal from "./ApprovalModal";
import { isCapabilityQuestion, capabilityReply, introChips, actionChips, followUpChips } from "../lib/chatPrompts";

interface Msg { role: "user" | "agent"; text: string; chips?: string[] }

const ACCOUNTS = [
  { id: "acc-checking-0001", label: "Checking (acc-checking-0001)" },
  { id: "acc-savings-0002", label: "Savings (acc-savings-0002)" }
];

const CUSTOMER_GREETING =
  "Hello! I'm your banking agent. I can check balances, show your transactions, " +
  "and move money between your accounts - transfers always wait for your approval. " +
  "Try \"transfer 50 to savings\" or \"what's my balance?\".";

const OPS_GREETING =
  "Reconciliation Console - diagnose ledger breaks with root-cause evidence. " +
  "Pick an account and ask to reconcile it, e.g. \"reconcile checking\".";

export default function AgentChat({ isEmployee, onAccountsChanged }: { isEmployee: boolean; onAccountsChanged?: () => void }) {
  const [messages, setMessages] = useState<Msg[]>([{ role: "agent", text: isEmployee ? OPS_GREETING : CUSTOMER_GREETING }]);
  const [input, setInput] = useState("");
  const [account, setAccount] = useState(ACCOUNTS[0].id);
  const [last, setLast] = useState<AgentResponse | null>(null);
  const [busy, setBusy] = useState(false);

  function buildMessage(text: string): string {
    const t = text.trim().toLowerCase();
    const isRead = t.includes("reconcile") || t.includes("balance") || t.includes("ledger") || t.includes("transaction") || t.includes("diagnos");
    const hasAccId = /acc-[a-z0-9-]+/.test(t);
    const hasType = t.includes("checking") || t.includes("savings");
    if (isEmployee && isRead && account && !hasAccId && !hasType) return text.trim() + " " + account;
    return text.trim();
  }

  async function send(text: string, body?: any, chips?: string[]) {
    setBusy(true);
    try {
      const json: any = await agentChat(body ?? { message: text });
      const data: AgentResponse = json?.data ?? json;
      setLast(data);
      const reply = data?.reply?.trim();
      if (reply) setMessages((m) => [...m, { role: "agent", text: reply, chips }]);
      return data;
    } finally { setBusy(false); }
  }

  function handlePrompt(text: string) {
    const t = text.trim();
    if (!t) return;
    setMessages((m) => [...m, { role: "user", text: t }]);
    setInput("");
    if (isCapabilityQuestion(t)) {
      setMessages((m) => [...m, { role: "agent", text: capabilityReply(isEmployee), chips: actionChips(isEmployee) }]);
      return;
    }
    const message = buildMessage(t);
    const history = messages.slice(-6).map((m) => `${m.role === "user" ? "User" : "Agent"}: ${m.text}`);
    send(message, { message, history }, followUpChips(isEmployee, t));
  }

  function onSend(e: React.FormEvent) {
    e.preventDefault();
    handlePrompt(input);
  }

  function onApprove() {
    if (!last) return;
    const ids = last.pendingSteps.map((s) => s.stepId);
    setMessages((m) => [...m, { role: "user", text: "Approved: " + ids.join(", ") }]);
    send("", { plan: last.plan, approval: ids }, followUpChips(isEmployee, "Approved: " + ids.join(", "))).then(() => onAccountsChanged?.());
  }

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-4xl mb-1">{isEmployee ? "Reconciliation Console" : "Your Agent"}</h1>
      <p className="text-muted mb-4">
        {isEmployee
          ? "Internal ops tool - employee only. Diagnose a ledger break and propose a corrective entry."
          : "Ask in plain language. Balances and history are yours alone; risky moves wait for your approval."}
      </p>
      {isEmployee && (
        <div className="flex items-center gap-3 mb-4">
          <label className="label">Account</label>
          <select className="field !w-auto" value={account} onChange={(e) => setAccount(e.target.value)}>
            {ACCOUNTS.map((a) => <option key={a.id} value={a.id}>{a.label}</option>)}
          </select>
        </div>
      )}
      <div className="glass relative rounded-[26px] h-[60vh] flex flex-col overflow-hidden shadow-card">
        <div className="flex-1 overflow-auto p-5 space-y-4">
          {messages.map((m, i) => (
            <div key={i} className={`flex ${m.role === "user" ? "justify-end" : "justify-start"} bubble-in`}>
              {m.role === "user" ? (
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
          <ApprovalModal steps={last.pendingSteps} onApprove={onApprove} onCancel={() => setLast(null)} />
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
          <input className="field" value={input} onChange={(e) => setInput(e.target.value)} placeholder={isEmployee ? "Diagnose an account..." : "Ask your agent..."} />
          <button className="btn btn-accent px-5" disabled={busy}>{busy ? "..." : "Send"}</button>
        </form>
      </div>
    </div>
  );
}