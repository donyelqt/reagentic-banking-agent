import { useEffect, useRef, useState } from "react";
import { agentChat, classifySpending, getLedger } from "../api";
import type { AccountView, AgentResponse } from "../types";
import ApprovalModal from "./ApprovalModal";
import { Brand } from "./Brand";
import { isCapabilityQuestion, capabilityReply, isAnalyzeQuestion, analyzeReply, actionChips, followUpChips } from "../lib/chatPrompts";

interface Msg { role: "user" | "agent"; text: string; chips?: string[] }

const FALLBACK_ACCOUNTS = [
  { id: "acc-checking-0001", label: "Checking (acc-checking-0001)" },
  { id: "acc-savings-0002", label: "Savings (acc-savings-0002)" }
];

function accountOptions(accounts?: AccountView[]) {
  if (accounts && accounts.length > 0) {
    return accounts.map((a) => ({
      id: a.accountId,
      label: `${a.type.charAt(0).toUpperCase() + a.type.slice(1).toLowerCase()} (${a.accountId})`
    }));
  }
  return FALLBACK_ACCOUNTS;
}

function greetingForNow(): string {
  const h = new Date().getHours();
  if (h < 5) return "Good night";
  if (h < 12) return "Good morning";
  if (h < 18) return "Good afternoon";
  return "Good evening";
}

export default function AgentChat({ isEmployee, onAccountsChanged, accounts }: { isEmployee: boolean; onAccountsChanged?: () => void; accounts?: AccountView[] }) {
  const [messages, setMessages] = useState<Msg[]>([]);
  const [input, setInput] = useState("");
  const [account, setAccount] = useState(accountOptions(accounts)[0].id);
  const [last, setLast] = useState<AgentResponse | null>(null);
  const [busy, setBusy] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const el = scrollRef.current;
    if (el) el.scrollTo({ top: el.scrollHeight, behavior: "smooth" });
  }, [messages, busy]);

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
    if (isAnalyzeQuestion(t) && !isEmployee) {
      handleAnalyze();
      return;
    }
    const message = buildMessage(t);
    const history = messages.slice(-6).map((m) => `${m.role === "user" ? "User" : "Agent"}: ${m.text}`);
    send(message, { message, history }, followUpChips(isEmployee, t));
  }

  async function handleAnalyze() {
    setBusy(true);
    try {
      const opts = accountOptions(accounts);
      const lists = await Promise.all(opts.map((a) => getLedger(a.id).then((r: any) => r.data ?? []).catch(() => [])));
      const summary = await classifySpending(lists.flat());
      setMessages((m) => [...m, { role: "agent", text: analyzeReply(summary), chips: followUpChips(false, "Analyze my spending") }]);
    } catch {
      setMessages((m) => [...m, { role: "agent", text: "I couldn't analyze your spending right now. Try again in a moment." }]);
    } finally {
      setBusy(false);
    }
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

  const heroTitle = isEmployee ? "Reconciliation Console" : `${greetingForNow()}, Baguio`;
  const heroDesc = isEmployee
    ? "Diagnose ledger breaks with root-cause evidence straight from the immutable ledger. Nothing here executes without your approval."
    : "I'm your private banking agent. I can check balances, show transactions, and move money between your accounts — transfers always wait for your approval.";
  const heroChips = ["What can you do?", ...actionChips(isEmployee)];

  return (
    <div className="flex flex-col min-h-0 h-[calc(100dvh_-_3.5rem)] md:h-[100dvh]">
      <h1 className="sr-only">{isEmployee ? "Reconciliation Console" : "Your Agent"}</h1>
      {isEmployee && (
        <div className="flex items-center gap-3 px-4 md:px-8 py-3 border-b border-line bg-surface/40">
          <label className="label" htmlFor="agent-account">Account</label>
          <select id="agent-account" className="field !w-auto" value={account} onChange={(e) => setAccount(e.target.value)}>
            {accountOptions(accounts).map((a) => <option key={a.id} value={a.id}>{a.label}</option>)}
          </select>
        </div>
      )}
      <div className="relative flex-1 min-h-0 flex flex-col">
        <div ref={scrollRef} className="flex-1 overflow-auto px-4 md:px-8 py-6">
          {messages.length === 0 ? (
            <div className="h-full grid place-items-center">
              <div className="w-full max-w-lg text-center">
                <div className="flex justify-center">
                  <div className="hero-brand"><Brand tone="light" /></div>
                </div>
                <p className="font-display text-3xl mt-6">{heroTitle}</p>
                <p className="text-muted text-base leading-relaxed mt-3">{heroDesc}</p>
                <div className="mt-7 flex flex-wrap justify-center gap-2.5">
                  {heroChips.map((c) => (
                    <button key={c} onClick={() => handlePrompt(c)} disabled={busy}
                      className="text-sm px-4 py-2 rounded-full border border-accent/35 bg-white text-ink shadow-soft hover:bg-accent hover:text-white hover:border-accent hover:-translate-y-0.5 transition disabled:opacity-50">
                      {c}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          ) : (
            <div className="space-y-4 max-w-3xl mx-auto w-full">
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
          )}
        </div>
        {last && last.pendingSteps.length > 0 && (
          <ApprovalModal steps={last.pendingSteps} accounts={accounts?.length ? accounts : undefined} onApprove={onApprove} onCancel={() => setLast(null)} busy={busy} />
        )}
        <form onSubmit={onSend} className="flex gap-2 p-3 md:p-4 border-t border-line bg-surface/60">
          <input className="field" value={input} onChange={(e) => setInput(e.target.value)} placeholder={isEmployee ? "Diagnose an account..." : "Ask your agent..."} />
          <button className="btn btn-accent px-5" disabled={busy}>{busy ? "..." : "Send"}</button>
        </form>
      </div>
    </div>
  );
}