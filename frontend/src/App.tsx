import { useEffect, useState } from "react";
import Landing from "./Landing";
import Login from "./components/Login";
import Dashboard from "./components/Dashboard";
import Transfer from "./components/Transfer";
import AgentChat from "./components/AgentChat";
import { Brand } from "./components/Brand";
import { getAccounts, sessionFromToken } from "./api";
import type { AccountView } from "./types";
import FloatingChat from "./components/FloatingChat";

type View = "dashboard" | "transfer" | "agent";

export default function App() {
  const [stage, setStage] = useState<"landing" | "app">("landing");
  const [token, setToken] = useState<string | null>(localStorage.getItem("jwt"));
  const [accounts, setAccounts] = useState<AccountView[]>([]);
  const session = sessionFromToken(token);
  const role = session?.role ?? "USER";
  const isEmployee = role === "EMPLOYEE";
  const navItems: View[] = isEmployee ? ["agent"] : ["dashboard", "transfer", "agent"];
  const [view, setView] = useState<View>(isEmployee ? "agent" : "dashboard");

  useEffect(() => {
    if (!token || stage !== "app") return;
    getAccounts().then((r: any) => setAccounts(r.data ?? [])).catch(() => setAccounts([]));
  }, [token, stage]);

  useEffect(() => {
    if (!navItems.includes(view)) setView(navItems[0] ?? "dashboard");
  }, [role]); // eslint-disable-line react-hooks/exhaustive-deps

  function logout() {
    localStorage.removeItem("jwt");
    setToken(null);
    setAccounts([]);
    setView("dashboard");
    setStage("landing");
  }

  if (stage === "landing") return <Landing onEnter={() => setStage("app")} />;
  if (!token) return <Login onLogin={(t) => { localStorage.setItem("jwt", t); setToken(t); }} />;

  return (
    <div className="min-h-screen bg-bg text-ink">
      <header className="sticky top-0 z-30 px-4 md:px-8 py-4">
        <div className="glass rounded-full px-5 py-3 flex items-center justify-between shadow-soft">
          <div className="flex items-center gap-5">
            <button onClick={() => setView(navItems[0] ?? "dashboard")} className="flex items-center">
              <Brand tone="light" />
            </button>
            <nav className="hidden md:flex items-center gap-1 bg-[#EDEBE3] rounded-full p-1">
              {navItems.map((v) => (
                <button key={v} onClick={() => setView(v)}
                  className={`capitalize px-4 py-1.5 rounded-full text-sm transition ${view === v ? "bg-accent text-white shadow-soft" : "text-muted hover:text-ink"}`}>
{v === "agent" ? (isEmployee ? "Ops Console" : "Agent") : v}
                </button>
              ))}
            </nav>
          </div>
          <div className="flex items-center gap-3">
            <span className="hidden sm:flex items-center gap-2 text-sm text-muted">
              <span className="w-8 h-8 rounded-full bg-accent text-white grid place-items-center text-xs font-semibold">{session?.email?.[0]?.toUpperCase() ?? "U"}</span>
              <span className="flex flex-col leading-tight">
                <span>{session?.email ?? "user"}</span>
                <span className="text-[10px] uppercase tracking-wide text-accent">{isEmployee ? "Ops Analyst" : "Customer"}</span>
              </span>
            </span>
            <button onClick={logout} className="btn btn-ghost !py-2 !px-4 text-sm">Sign out</button>
          </div>
        </div>
        <nav className="md:hidden mt-3 glass rounded-full p-1 flex justify-between">
          {navItems.map((v) => (
            <button key={v} onClick={() => setView(v)}
              className={`flex-1 capitalize py-2 rounded-full text-sm transition ${view === v ? "bg-accent text-white shadow-soft" : "text-muted"}`}>{v === "agent" ? "Ops Console" : v}</button>
          ))}
        </nav>
      </header>

      <main className="px-4 md:px-8 pb-16 max-w-6xl mx-auto pt-6">
        <div key={view} className="view-in">
          {view === "dashboard" && <Dashboard accounts={accounts} onTransfer={() => setView("transfer")} />}
          {view === "transfer" && <Transfer accounts={accounts} onDone={() => { getAccounts().then((r: any) => setAccounts(r.data ?? [])).catch(() => {}); setView("dashboard"); }} />}
          {view === "agent" && <AgentChat isEmployee={isEmployee} onAccountsChanged={() => getAccounts().then((r: any) => setAccounts(r.data ?? [])).catch(() => {})} />}
        </div>
      </main>
      {view !== 'agent' && <FloatingChat onExpand={() => setView('agent')} />}
    </div>
  );
}
