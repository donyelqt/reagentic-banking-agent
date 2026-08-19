import type { ReactNode } from "react";
import { NavLink } from "react-router-dom";
import { Brand } from "../Brand";

function DashboardIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" className="h-5 w-5" aria-hidden="true">
      <rect x="3" y="3" width="7" height="9" rx="1.5" />
      <rect x="14" y="3" width="7" height="5" rx="1.5" />
      <rect x="14" y="12" width="7" height="9" rx="1.5" />
      <rect x="3" y="16" width="7" height="5" rx="1.5" />
    </svg>
  );
}

function TransferIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" className="h-5 w-5" aria-hidden="true">
      <path d="M4 9h13l-3-3M20 15H7l3 3" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function ActivityIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" className="h-5 w-5" aria-hidden="true">
      <path d="M3 12h4l2.5-6 4 11 2.5-5H21" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function AgentIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" className="h-5 w-5" aria-hidden="true">
      <path d="M12 3l1.6 4.2L18 9l-4.4 1.8L12 15l-1.6-4.2L6 9l4.4-1.8L12 3z" strokeLinejoin="round" />
      <path d="M18 15l.8 2.1L21 18l-2.2.9L18 21l-.8-2.1L15 18l2.2-.9L18 15z" strokeLinejoin="round" />
    </svg>
  );
}

function CloseIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" className="h-5 w-5" aria-hidden="true">
      <path d="M6 6l12 12M18 6L6 18" strokeLinecap="round" />
    </svg>
  );
}

function MenuIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" className="h-5 w-5" aria-hidden="true">
      <path d="M4 7h16M4 12h16M4 17h16" strokeLinecap="round" />
    </svg>
  );
}

type NavItem = { to: string; label: string; icon: ReactNode };

function itemsFor(isEmployee: boolean): NavItem[] {
  if (isEmployee) return [{ to: "/agent", label: "Ops Console", icon: <AgentIcon /> }];
  return [
    { to: "/dashboard", label: "Dashboard", icon: <DashboardIcon /> },
    { to: "/activity", label: "Activity", icon: <ActivityIcon /> },
    { to: "/transfer", label: "Transfer", icon: <TransferIcon /> },
    { to: "/agent", label: "Agent", icon: <AgentIcon /> },
  ];
}

export function SidebarNav({ isEmployee, collapsed = false, onNavigate }: { isEmployee: boolean; collapsed?: boolean; onNavigate?: () => void }) {
  return (
    <nav aria-label="Primary" className="flex-1 px-3 py-4">
      <ul role="list" className="space-y-1">
        {itemsFor(isEmployee).map((it) => (
          <li key={it.to}>
            <NavLink
              to={it.to}
              onClick={onNavigate}
              title={collapsed ? it.label : undefined}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition ${
                  isActive ? "bg-accent text-white shadow-soft" : "text-muted hover:text-ink hover:bg-line/40"
                } ${collapsed ? "justify-center" : ""}`
              }
            >
              <span className="shrink-0">{it.icon}</span>
              {!collapsed && <span className="truncate">{it.label}</span>}
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  );
}

export function Sidebar({
  isEmployee,
  email,
  roleLabel,
  collapsed,
  onToggleCollapse,
  onLogout,
}: {
  isEmployee: boolean;
  email: string;
  roleLabel: string;
  collapsed: boolean;
  onToggleCollapse: () => void;
  onLogout: () => void;
}) {
  return (
    <aside
      className={`hidden md:flex flex-col h-screen sticky top-0 border-r border-line bg-bg shrink-0 transition-[width] duration-200 ${
        collapsed ? "w-16" : "w-60"
      }`}
    >
      <div className="flex items-center justify-between h-16 px-4 border-b border-line">
        {collapsed ? (
          <span className="mx-auto font-display text-accent text-lg">R</span>
        ) : (
          <Brand tone="light" />
        )}
        <button
          onClick={onToggleCollapse}
          aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
          className="btn btn-ghost !p-2 !px-2 text-muted"
        >
          {collapsed ? <MenuIcon /> : <CloseIcon />}
        </button>
      </div>

      <SidebarNav isEmployee={isEmployee} collapsed={collapsed} />

      <div className="border-t border-line p-3">
        <div className="flex items-center gap-3 px-1 py-2">
          <span className="w-8 h-8 rounded-full bg-accent text-white grid place-items-center text-xs font-semibold shrink-0">
            {email?.[0]?.toUpperCase() ?? "U"}
          </span>
          {!collapsed && (
            <span className="min-w-0 flex-1">
              <span className="block truncate text-sm">{email}</span>
              <span className="block text-[10px] uppercase tracking-wide text-accent">{roleLabel}</span>
            </span>
          )}
        </div>
        {!collapsed && (
          <button onClick={onLogout} className="btn btn-ghost w-full mt-1">
            Sign out
          </button>
        )}
      </div>
    </aside>
  );
}

export { MenuIcon };
