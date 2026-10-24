import { useEffect, useState } from "react";
import {
  applyMotionPreference,
  effectiveReduceMotion,
  NONE_ACCOUNT,
  readPreferences,
  systemPrefersReducedMotion,
  writePreferences,
  type Preferences,
} from "../../lib/preferences";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../ui/select";
import type { AccountView } from "../../types";

export default function PreferencesSection({
  email,
  accounts,
}: {
  email: string;
  accounts: AccountView[];
}) {
  const [preferences, setPreferences] = useState<Preferences>(() => readPreferences(email));
  const [message, setMessage] = useState<{ kind: "ok" | "err"; text: string } | null>(null);

  useEffect(() => {
    setPreferences(readPreferences(email));
    setMessage(null);
  }, [email]);

  function update(next: Partial<Preferences>, ok: string) {
    const merged: Preferences = { ...preferences, ...next };
    const stored = writePreferences(email, merged);
    if (!stored) {
      setMessage({ kind: "err", text: "We couldn't save that preference. Try again." });
      return;
    }
    setPreferences(merged);
    if (next.reduceMotion !== undefined) applyMotionPreference(merged);
    setMessage({ kind: "ok", text: ok });
  }

  const selected = preferences.defaultAccountId ?? NONE_ACCOUNT;
  const stored = preferences.defaultAccountId;
  const known = stored ? accounts.some((account) => account.accountId === stored) : true;
  const missing = stored && !known ? stored : null;
  const reduced = effectiveReduceMotion(preferences);
  const systemReduced = systemPrefersReducedMotion();

  return (
    <section id="preferences" aria-labelledby="preferences-heading" className="scroll-mt-24">
      <div className="mb-4">
        <h2 id="preferences-heading" className="text-2xl">Preferences</h2>
        <p className="mt-1 text-sm text-muted">Tune how Reagentic behaves for you.</p>
      </div>

      <div className="card p-6 md:p-8">
        <div>
          <h3 className="text-xl">Default account</h3>
          <p className="mt-1 text-sm text-muted">
            Used first when you start a transfer or open Activity.
          </p>
          <div className="mt-4 max-w-sm">
            <label className="label" htmlFor="defaultAccount">Default account</label>
            <Select
              value={selected}
              onValueChange={(value) =>
                update(
                  { defaultAccountId: value === NONE_ACCOUNT ? null : value },
                  "Default account saved.",
                )
              }
            >
              <SelectTrigger id="defaultAccount" aria-label="Default account" className="mt-1.5 w-full">
                <SelectValue placeholder="No default account" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={NONE_ACCOUNT}>No default account</SelectItem>
                {missing && (
                  <SelectItem value={missing}>Unavailable account ({missing})</SelectItem>
                )}
                {accounts.map((account) => (
                  <SelectItem key={account.accountId} value={account.accountId}>
                    {account.type} ({account.accountId})
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>

        <div className="mt-8 border-t border-line pt-8">
          <h3 className="text-xl">Motion</h3>
          <p className="mt-1 text-sm text-muted">Turn off animated transitions and movement.</p>
          <div className="mt-5 flex max-w-sm items-center justify-between gap-4">
            <div>
              <p id="reduceMotion-label" className="text-sm font-medium">Reduce motion</p>
              <p className="mt-1 text-xs text-muted">{motionCopy(reduced, preferences.reduceMotion, systemReduced)}</p>
              {preferences.reduceMotion !== null && (
                <button
                  type="button"
                  onClick={() => update({ reduceMotion: null }, "Motion now follows your system setting.")}
                  className="mt-2 text-xs font-medium text-accent transition hover:underline"
                >
                  Use my system setting
                </button>
              )}
            </div>
            <button
              type="button"
              role="switch"
              aria-checked={reduced}
              aria-labelledby="reduceMotion-label"
              onClick={() =>
                update(
                  { reduceMotion: !reduced },
                  reduced ? "Motion turned back on." : "Motion turned off.",
                )
              }
              className={`relative h-6 w-11 shrink-0 rounded-full transition-colors ${
                reduced ? "bg-accent" : "bg-line"
              }`}
            >
              <span
                aria-hidden="true"
                className={`absolute top-0.5 h-5 w-5 rounded-full bg-white shadow-soft transition-transform ${
                  reduced ? "translate-x-[22px]" : "translate-x-0.5"
                }`}
              />
            </button>
          </div>
        </div>

        <div className="mt-8 border-t border-line pt-6">
          <div role="status" aria-live="polite">
            {message && (
              <p className={`text-sm ${message.kind === "ok" ? "text-pos" : "text-neg"}`}>{message.text}</p>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}

function motionCopy(reduced: boolean, stored: boolean | null, systemReduced: boolean) {
  if (stored === null) return "Animations follow your system setting.";
  if (stored && reduced) return "Animations and transitions are off.";
  if (!stored && systemReduced) return "Animations are on, overriding your system's reduced motion setting.";
  return "Animations and transitions are on.";
}
