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
