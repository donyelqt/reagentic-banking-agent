import { QueryClient, useQuery, useQueries } from "@tanstack/react-query";
import { getAccounts, getLedger } from "../api";
import type { AccountView } from "../types";

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      gcTime: 5 * 60_000,
      retry: 1,
      refetchOnWindowFocus: false,
      refetchOnReconnect: false,
    },
  },
});

export const qk = {
  accounts: ["accounts"] as const,
  ledger: (accountId: string) => ["ledger", accountId] as const,
} as const;

export function useAccounts(enabled = true) {
  return useQuery({
    queryKey: qk.accounts,
    queryFn: () => getAccounts().then((r) => r.data ?? []),
    enabled,
    staleTime: 30_000,
  });
}

export function useLedgers(accountIds: string[], enabled = true) {
  return useQueries({
    queries: accountIds.map((id) => ({
      queryKey: qk.ledger(id),
      queryFn: () => getLedger(id).then((r) => r.data ?? []),
      enabled: enabled && !!id,
      staleTime: 15_000,
    })),
  });
}

// helper to derive allActivity from ledgers
export function useAllLedgers(accounts: AccountView[], enabled = true) {
  const ids = accounts.map((a) => a.accountId);
  const results = useLedgers(ids, enabled && ids.length > 0);
  const data = results.flatMap((r) => r.data ?? []);
  const isLoading = results.some((r) => r.isLoading);
  const isError = results.some((r) => r.isError);
  return { data, isLoading, isError, results };
}
