export type Preferences = {
  defaultAccountId: string | null;
  reduceMotion: boolean;
};

const STORAGE_PREFIX = "reagentic.preferences.";

export const NONE_ACCOUNT = "__none__";

export function preferencesKey(email: string) {
  return `${STORAGE_PREFIX}${email}`;
}

export function systemPrefersReducedMotion() {
  if (typeof window === "undefined" || typeof window.matchMedia !== "function") return false;
  return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

export function readPreferences(email: string): Preferences {
  const fallback: Preferences = {
    defaultAccountId: null,
    reduceMotion: systemPrefersReducedMotion(),
  };
  if (!email) return fallback;
  try {
    const raw = window.localStorage.getItem(preferencesKey(email));
    if (!raw) return fallback;
    const parsed = JSON.parse(raw) as Partial<Preferences>;
    return {
      defaultAccountId:
        typeof parsed.defaultAccountId === "string" ? parsed.defaultAccountId : null,
      reduceMotion:
        typeof parsed.reduceMotion === "boolean" ? parsed.reduceMotion : fallback.reduceMotion,
    };
  } catch {
    return fallback;
  }
}

export function writePreferences(email: string, preferences: Preferences) {
  if (!email) return;
  try {
    window.localStorage.setItem(preferencesKey(email), JSON.stringify(preferences));
  } catch {
    return;
  }
}

export function applyReducedMotion(reduceMotion: boolean) {
  const root = document.documentElement;
  if (reduceMotion) root.setAttribute("data-reduce-motion", "true");
  else root.removeAttribute("data-reduce-motion");
}

export function preferredAccountId(email: string, accounts: { accountId: string }[]) {
  const preferred = readPreferences(email).defaultAccountId;
  if (!preferred) return null;
  return accounts.some((account) => account.accountId === preferred) ? preferred : null;
}
