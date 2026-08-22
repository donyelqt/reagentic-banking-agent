import { useEffect, useState } from "react";
import { BrowserRouter, Routes, Route, Navigate, useNavigate, useLocation } from "react-router-dom";
import Landing from "./Landing";
import Login from "./components/Login";
import Dashboard from "./components/Dashboard";
import Transfer from "./components/Transfer";
import ActivityPage from "./components/ActivityPage";
import LedgerConsole from "./components/LedgerConsole";
import AgentChat from "./components/AgentChat";
import Settings from "./components/Settings";
import { Brand } from "./components/Brand";
import { getAccounts, sessionFromToken } from "./api";
import type { AccountView } from "./types";
import FloatingChat from "./components/FloatingChat";
import { Sidebar, SidebarNav, MenuIcon } from "./components/Sidebar/Sidebar";

type Stage = "landing" | "app";

export default function App() {
  const [stage, setStage] = useState<Stage>("landing");
  const [token, setToken] = useState<string | null>(localStorage.getItem("jwt"));
  const [accounts, setAccounts] = useState<AccountView[]>([]);
  const session = sessionFromToken(token);
  const role = session?.role ?? "USER";
  const isEmployee = role === "EMPLOYEE";
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [collapsed, setCollapsed] = useState(() => localStorage.getItem("sidebar-collapsed") === "1");

  useEffect(() => {
    if (!token || stage !== "app") return;
    getAccounts().then((r: any) => setAccounts(r.data ?? [])).catch(() => setAccounts([]));
  }, [token, stage]);

  function logout() {
    localStorage.removeItem("jwt");
    setToken(null);
    setAccounts([]);
    setDrawerOpen(false);
    setStage("landing");
  }

  function refreshAccounts() {
    getAccounts().then((r: any) => setAccounts(r.data ?? [])).catch(() => {});
  }

  if (stage === "landing") return <Landing onEnter={() => setStage("app")} />;
  if (!token) return <Login onLogin={(t) => {
    localStorage.setItem("jwt", t);
    window.history.replaceState(null, "", "/");
    setToken(t);
  }} />;

  return (
    <BrowserRouter>
      <AppShell
        isEmployee={isEmployee}
        email={session?.email ?? "user"}
        roleLabel={isEmployee ? "Ops Analyst" : "Customer"}
        accounts={accounts}
        collapsed={collapsed}
        onToggleCollapse={() => {
          const next = !collapsed;
          setCollapsed(next);
          localStorage.setItem("sidebar-collapsed", next ? "1" : "0");
        }}
        drawerOpen={drawerOpen}
        setDrawerOpen={setDrawerOpen}
        onLogout={logout}
        onAccountsChanged={refreshAccounts}
      />
    </BrowserRouter>
  );
}

function AppShell({
  isEmployee,
  email,
  roleLabel,
  accounts,
  collapsed,
  onToggleCollapse,
  drawerOpen,
  setDrawerOpen,
  onLogout,
  onAccountsChanged,
}: {
  isEmployee: boolean;
  email: string;
  roleLabel: string;
  accounts: AccountView[];
  collapsed: boolean;
  onToggleCollapse: () => void;
  drawerOpen: boolean;
  setDrawerOpen: (v: boolean) => void;
  onLogout: () => void;
  onAccountsChanged: () => void;
}) {
  const location = useLocation();
  const navigate = useNavigate();
  const onAgent = location.pathname === "/agent";

  return (
    <div className="flex min-h-screen bg-bg text-ink">
      <a href="#main" className="skip-link">Skip to content</a>
      <Sidebar
        isEmployee={isEmployee}
        email={email}
        roleLabel={roleLabel}
        collapsed={collapsed}
        onToggleCollapse={onToggleCollapse}
        onLogout={onLogout}
      />

      {drawerOpen && (
        <div className="fixed inset-0 z-40 md:hidden">
          <div className="absolute inset-0 bg-black/40" aria-hidden="true" onClick={() => setDrawerOpen(false)} />
          <div
            className="absolute inset-y-0 left-0 w-64 bg-bg border-r border-line flex flex-col"
            role="dialog"
            aria-modal="true"
            aria-label="Navigation"
          >
            <div className="flex items-center justify-between h-16 px-4 border-b border-line">
              <Brand tone="light" />
              <button aria-label="Close navigation" onClick={() => setDrawerOpen(false)} className="btn btn-ghost !p-2 text-muted">
                <span aria-hidden="true">✕</span>
              </button>
            </div>
            <SidebarNav isEmployee={isEmployee} onNavigate={() => setDrawerOpen(false)} />
            <div className="border-t border-line p-3">
              <div className="flex items-center gap-3 px-1 py-2">
                <span className="w-8 h-8 rounded-full bg-accent text-white grid place-items-center text-xs font-semibold shrink-0">
                  {email?.[0]?.toUpperCase() ?? "U"}
                </span>
                <span className="min-w-0 flex-1">
                  <span className="block truncate text-sm">{email}</span>
                  <span className="block text-[10px] uppercase tracking-wide text-accent">{roleLabel}</span>
                </span>
              </div>
              <button
                onClick={() => {
                  setDrawerOpen(false);
                  onLogout();
                }}
                className="btn btn-ghost w-full mt-1"
              >
                Sign out
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="flex-1 flex flex-col min-w-0">
        <div className="md:hidden sticky top-0 z-30 flex items-center gap-3 px-4 h-14 glass border-b border-line">
          <button
            aria-label="Open navigation"
            aria-expanded={drawerOpen}
            onClick={() => setDrawerOpen(true)}
            className="btn btn-ghost !p-2 text-muted"
          >
            <MenuIcon />
          </button>
          <Brand tone="light" />
        </div>

        <main
          id="main"
          className={
            onAgent
              ? "flex-1 flex flex-col min-h-0 w-full"
              : "px-4 md:px-8 pb-16 max-w-6xl mx-auto pt-6 w-full"
          }
        >
          <Routes>
            <Route path="/" element={<Navigate to={isEmployee ? "/agent" : "/dashboard"} replace />} />
            <Route
              path="/dashboard"
              element={
                isEmployee ? (
                  <Navigate to="/agent" replace />
                ) : (
                  <Dashboard accounts={accounts} onTransfer={() => navigate("/transfer")} onViewAll={() => navigate("/activity")} />
                )
              }
            />
            <Route
              path="/activity"
              element={
                isEmployee ? (
                  <Navigate to="/agent" replace />
                ) : (
                  <ActivityPage accounts={accounts} />
                )
              }
            />
            <Route
              path="/transfer"
              element={
                isEmployee ? (
                  <Navigate to="/agent" replace />
                ) : (
                  <Transfer
                    accounts={accounts}
                    onDone={() => {
                      onAccountsChanged();
                      navigate("/dashboard");
                    }}
                  />
                )
              }
            />
            <Route path="/agent" element={<AgentChat isEmployee={isEmployee} accounts={accounts} onAccountsChanged={onAccountsChanged} />} />
            <Route path="/settings" element={<Settings accounts={accounts} />} />
            <Route
              path="/ops"
              element={
                isEmployee ? (
                  <LedgerConsole />
                ) : (
                  <Navigate to="/dashboard" replace />
                )
              }
            />
            <Route path="*" element={<Navigate to={isEmployee ? "/agent" : "/dashboard"} replace />} />
          </Routes>
        </main>
      </div>

      {!onAgent && <FloatingChat accounts={accounts} onExpand={() => navigate("/agent")} />}
    </div>
  );
}
