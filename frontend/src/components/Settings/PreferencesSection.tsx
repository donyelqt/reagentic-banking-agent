import { useEffect, useState } from "react";
import {
  applyReducedMotion,
  NONE_ACCOUNT,
  readPreferences,
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
  const [saved, setSaved] = useState<string | null>(null);

  useEffect(() => {
    setPreferences(readPreferences(email));
    setSaved(null);
  }, [email]);

  function update(next: Partial<Preferences>, message: string) {
    const merged: Preferences = { ...preferences, ...next };
    setPreferences(merged);
    writePreferences(email, merged);
    if (next.reduceMotion !== undefined) applyReducedMotion(merged.reduceMotion);
    setSaved(message);
  }

  const selected = preferences.defaultAccountId ?? NONE_ACCOUNT;
  const stored = preferences.defaultAccountId;
  const known = stored ? accounts.some((account) => account.accountId === stored) : true;
  const missing = stored && !known ? stored : null;

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
              <p className="mt-1 text-xs text-muted">
                {preferences.reduceMotion
                  ? "Animations and transitions are off."
                  : "Animations follow your system setting."}
              </p>
            </div>
            <button
              type="button"
              role="switch"
              aria-checked={preferences.reduceMotion}
              aria-labelledby="reduceMotion-label"
              onClick={() =>
                update(
                  { reduceMotion: !preferences.reduceMotion },
                  preferences.reduceMotion ? "Motion turned back on." : "Motion turned off.",
                )
              }
              className={`relative h-6 w-11 shrink-0 rounded-full transition-colors ${
                preferences.reduceMotion ? "bg-accent" : "bg-line"
              }`}
            >
              <span
                aria-hidden="true"
                className={`absolute top-0.5 h-5 w-5 rounded-full bg-white shadow-soft transition-transform ${
                  preferences.reduceMotion ? "translate-x-[22px]" : "translate-x-0.5"
                }`}
              />
            </button>
          </div>
        </div>

        <div className="mt-8 border-t border-line pt-6">
          <div role="status" aria-live="polite">
            {saved && <p className="text-sm text-pos">{saved}</p>}
          </div>
        </div>
      </div>
    </section>
  );
}
