export type Preferences = {
  defaultAccountId: string | null;
  reduceMotion: boolean | null;
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
  const fallback: Preferences = { defaultAccountId: null, reduceMotion: null };
  if (!email) return fallback;
  try {
    const raw = window.localStorage.getItem(preferencesKey(email));
    if (!raw) return fallback;
    const parsed = JSON.parse(raw) as Partial<Preferences>;
    return {
      defaultAccountId:
        typeof parsed.defaultAccountId === "string" && parsed.defaultAccountId.length > 0
          ? parsed.defaultAccountId
          : null,
      reduceMotion: typeof parsed.reduceMotion === "boolean" ? parsed.reduceMotion : null,
    };
  } catch {
    return fallback;
  }
}

export function writePreferences(email: string, preferences: Preferences) {
  if (!email) return false;
  try {
    window.localStorage.setItem(preferencesKey(email), JSON.stringify(preferences));
    return true;
  } catch {
    return false;
  }
}

export function effectiveReduceMotion(preferences: Preferences) {
  return preferences.reduceMotion ?? systemPrefersReducedMotion();
}

export function applyMotionPreference(preferences: Preferences) {
  const root = document.documentElement;
  if (preferences.reduceMotion === null) root.removeAttribute("data-reduce-motion");
  else root.setAttribute("data-reduce-motion", preferences.reduceMotion ? "true" : "false");
}

export function reducedMotionEnabled() {
  if (typeof document === "undefined") return false;
  const attribute = document.documentElement.getAttribute("data-reduce-motion");
  if (attribute === "true") return true;
  if (attribute === "false") return false;
  return systemPrefersReducedMotion();
}

export function preferredAccountId(email: string, accounts: { accountId: string }[]) {
  const preferred = readPreferences(email).defaultAccountId;
  if (!preferred) return null;
  return accounts.some((account) => account.accountId === preferred) ? preferred : null;
}
