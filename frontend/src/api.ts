import type { AccountView, AgentResponse, CategorySpend, ChatRequest, LedgerEntry, ReconcileResult } from "./types";

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

export const getInternalLedger = (accountId: string) =>
  req<{ success: boolean; data: LedgerEntry[] }>("/api/ledger/internal/" + accountId);

export const reconcileAccount = (accountId: string) =>
  req<{ success: boolean; data: ReconcileResult }>("/api/agent/reconcile/" + accountId);

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

const CLASSIFY_BATCH = 100;

/**
 * Classifies ledger entries into a spending-by-category summary.
 * Only money out with a merchant description is spending: live transfers
 * (no description) and credits (income) are excluded. The classify endpoint
 * caps at 100 items per request, so the ledger is chunked and the category
 * summaries are merged client-side.
 */
export async function classifySpending(entries: LedgerEntry[]): Promise<CategorySpend[]> {
  const items = entries
    .filter((e) => e.description && parseFloat(e.signedAmount) < 0)
    .map((e) => ({ description: e.description as string, amount: e.signedAmount }));
  if (items.length === 0) return [];

  const merged = new Map<string, { total: number; count: number }>();
  for (let i = 0; i < items.length; i += CLASSIFY_BATCH) {
    const json: any = await req("/api/agent/classify", {
      method: "POST",
      body: JSON.stringify({ transactions: items.slice(i, i + CLASSIFY_BATCH) })
    });
    const data = json?.data ?? json;
    for (const t of data?.summary ?? []) {
      const cur = merged.get(t.category) ?? { total: 0, count: 0 };
      cur.total += Math.abs(parseFloat(t.total) || 0);
      cur.count += t.count ?? 0;
      merged.set(t.category, cur);
    }
  }

  return [...merged.entries()]
    .map(([category, v]) => ({ category, total: v.total, count: v.count }))
    .sort((a, b) => b.total - a.total);
}

/**
 * Classifies each spending entry of an account into its category.
 * The classify endpoint echoes classified transactions in request order
 * (deterministic keyword classifier is the no-key default), so batches are
 * joined positionally. If a batch ever fails to echo, the entry simply gets
 * no chip rather than crashing the page.
 */
export async function classifyEntries(entries: LedgerEntry[]): Promise<Map<number, string>> {
  const spend = entries.filter((e) => e.description && parseFloat(e.signedAmount) < 0);
  const result = new Map<number, string>();
  if (spend.length === 0) return result;

  for (let i = 0; i < spend.length; i += CLASSIFY_BATCH) {
    const batch = spend.slice(i, i + CLASSIFY_BATCH);
    try {
      const json: any = await req("/api/agent/classify", {
        method: "POST",
        body: JSON.stringify({
          transactions: batch.map((e) => ({ description: e.description, amount: e.signedAmount }))
        })
      });
      const data = json?.data ?? json;
      const items: { category?: string }[] = data?.transactions ?? [];
      if (items.length !== batch.length) continue;
      batch.forEach((e, idx) => {
        const category = items[idx]?.category;
        if (category) result.set(e.entryId, category);
      });
    } catch {
      continue;
    }
  }
  return result;
}

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
