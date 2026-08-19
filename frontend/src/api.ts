import type { AccountView, AgentResponse, ChatRequest } from "./types";

const API = import.meta.env.VITE_GATEWAY_URL || "";

async function req<T = any>(path: string, opts: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem("jwt");
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(opts.headers as Record<string, string>)
  };
  if (token) headers["Authorization"] = "Bearer " + token;
  const res = await fetch(API + path, { ...opts, headers });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || "HTTP " + res.status);
  }
  return res.json();
}

export async function login(username: string, password: string): Promise<string> {
  const json = await req("/api/auth/login", {
    method: "POST",
    body: JSON.stringify({ email: username, password })
  });
  const data = json.data ?? json;
  const token = data.token ?? json.token;
  if (!token) throw new Error("No token in login response");
  return token;
}

export const getAccounts = () => req<{ success: boolean; data: AccountView[] }>("/api/accounts");

export const transfer = (body: {
  sourceAccountId: string;
  destinationAccountId: string;
  amount: string;
  idempotencyKey: string;
}) => req("/api/payments/transfer", { method: "POST", body: JSON.stringify(body) });

export const getLedger = (accountId: string) =>
  req<{ success: boolean; data: any[] }>("/api/ledger/" + accountId);

async function downloadFile(path: string, filename: string): Promise<void> {
  const token = localStorage.getItem("jwt");
  const res = await fetch(API + path, {
    headers: token ? { Authorization: "Bearer " + token } : {}
  });
  if (!res.ok) throw new Error("Download failed: HTTP " + res.status);
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

export const downloadStatementCsv = (accountId: string) =>
  downloadFile(`/api/ledger/${accountId}/statement.csv`, `statement-${accountId}.csv`);

export const downloadStatementExcel = (accountId: string) =>
  downloadFile(`/api/ledger/${accountId}/statement.xlsx`, `statement-${accountId}.xlsx`);

export const agentChat = (body: ChatRequest) =>
  req<AgentResponse>("/api/agent/chat", { method: "POST", body: JSON.stringify(body) });

export function sessionFromToken(token: string | null): { email: string; role: string } | null {
  if (!token) return null;
  try {
    const part = token.split(".")[1];
    const json = JSON.parse(atob(part.replace(/-/g, "+").replace(/_/g, "/")));
    return { email: json.sub, role: json.role };
  } catch {
    return null;
  }
}
