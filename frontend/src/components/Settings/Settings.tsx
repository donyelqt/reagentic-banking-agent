import { useEffect, useRef } from "react";
import { useProfile } from "../../lib/queries";
import type { Session } from "../../api";
import type { AccountView, UserProfile } from "../../types";
import ProfileSection from "./ProfileSection";
import SecuritySection from "./SecuritySection";
import PreferencesSection from "./PreferencesSection";

export default function Settings({
  email,
  session,
  accounts,
  onSignOut,
}: {
  email: string;
  session: Session | null;
  accounts: AccountView[];
  onSignOut: () => void;
}) {
  const { data: profile, isLoading, isError, refetch, isFetching } = useProfile(email);
  const retryRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!isFetching && isError && !profile) retryRef.current?.focus();
  }, [isFetching, isError, profile]);

  return (
    <div className="view-in">
      <header className="border-b border-line pb-8">
        <p className="label">Account</p>
        <h1 className="mt-2 text-3xl md:text-4xl">Settings</h1>
        <p className="mt-2 max-w-xl text-sm text-muted md:text-base">
          Your profile, sign-in details, and preferences.
        </p>
      </header>

      {!profile && isLoading ? (
        <SettingsSkeleton />
      ) : !profile ? (
        <div role="alert" className="card mt-8 p-6 md:p-8">
          <h2 className="text-xl">We couldn't load your settings</h2>
          <p className="mt-2 text-sm text-muted">Your details didn't come through. Try again.</p>
          <button
            ref={retryRef}
            type="button"
            onClick={() => refetch()}
            disabled={isFetching}
            className="btn btn-ghost mt-6 active:scale-[.98] disabled:cursor-not-allowed disabled:opacity-50"
          >
            {isFetching ? "Trying..." : "Try again"}
          </button>
        </div>
      ) : (
        <>
          {isError && (
            <div role="status" className="mt-6 flex flex-wrap items-center justify-between gap-3 rounded-xl bg-line/40 px-4 py-3 text-sm">
              <p className="text-muted">We couldn't refresh your details. Showing the last saved version.</p>
              <button
                type="button"
                onClick={() => refetch()}
                disabled={isFetching}
                className="font-medium text-accent hover:underline disabled:opacity-50"
              >
                {isFetching ? "Retrying..." : "Retry"}
              </button>
            </div>
          )}

          <div className="grid gap-6 pt-8 lg:grid-cols-[240px_minmax(0,1fr)] lg:gap-10">
            <aside className="self-start lg:sticky lg:top-8">
              <div className="card p-5">
                <div className="flex items-center gap-3">
                  <span
                    aria-hidden="true"
                    className="grid h-11 w-11 shrink-0 place-items-center rounded-full bg-accent font-display text-lg text-white"
                  >
                    {initial(profile)}
                  </span>
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium">{profile.fullName?.trim() || "Your profile"}</p>
                    <p className="truncate text-xs text-muted">{profile.email}</p>
                  </div>
                </div>
                <span className="chip mt-4 text-[11px] uppercase tracking-[.12em]">{roleLabel(profile.role)}</span>
              </div>

              <nav aria-label="Settings sections" className="mt-6 px-1">
                <p className="text-[11px] font-medium uppercase tracking-[.14em] text-muted">On this page</p>
                <a
                  href="#profile"
                  className="mt-3 block border-l-2 border-accent pl-3 text-sm font-medium text-ink"
                >
                  Profile
                </a>
                <a
                  href="#security"
                  className="mt-3 block border-l-2 border-transparent pl-3 text-sm font-medium text-muted transition hover:border-line hover:text-ink"
                >
                  Security
                </a>
                <a
                  href="#preferences"
                  className="mt-3 block border-l-2 border-transparent pl-3 text-sm font-medium text-muted transition hover:border-line hover:text-ink"
                >
                  Preferences
                </a>
              </nav>
            </aside>

            <div className="min-w-0 space-y-12">
              <ProfileSection profile={profile} />
              <SecuritySection
                profile={profile}
                session={session}
                roleLabel={roleLabel(session?.role ?? profile.role)}
                onSignOut={onSignOut}
              />
              <PreferencesSection email={email} accounts={accounts} />
            </div>
          </div>
        </>
      )}
    </div>
  );
}

function roleLabel(role: string) {
  if (role === "EMPLOYEE") return "Ops analyst";
  if (role === "USER") return "Customer";
  return "Account holder";
}

function initial(profile: UserProfile) {
  const source = profile.fullName?.trim() || profile.email;
  return source.charAt(0).toUpperCase();
}

function SettingsSkeleton() {
  return (
    <>
      <p role="status" className="sr-only">Loading settings</p>
      <div aria-hidden="true" className="grid gap-6 pt-8 lg:grid-cols-[240px_minmax(0,1fr)] lg:gap-10">
        <div className="card p-5">
          <div className="flex items-center gap-3">
            <div className="h-11 w-11 shrink-0 rounded-full shimmer" />
            <div className="flex-1 space-y-2">
              <div className="h-3.5 w-24 rounded shimmer" />
              <div className="h-3 w-36 rounded shimmer" />
            </div>
          </div>
          <div className="mt-4 h-6 w-24 rounded-full shimmer" />
        </div>

        <div className="card p-6 md:p-8">
          <div className="h-5 w-24 rounded shimmer" />
          <div className="mt-3 h-3 w-64 max-w-full rounded shimmer" />
          <div className="mt-6 grid gap-5 sm:grid-cols-2">
            <div className="space-y-3 sm:col-span-2">
              <div className="h-3 w-20 rounded shimmer" />
              <div className="h-[52px] rounded-[14px] shimmer" />
            </div>
            <div className="space-y-3">
              <div className="h-3 w-16 rounded shimmer" />
              <div className="h-[52px] rounded-[14px] shimmer" />
            </div>
            <div className="space-y-3">
              <div className="h-3 w-16 rounded shimmer" />
              <div className="h-[52px] rounded-[14px] shimmer" />
            </div>
          </div>
          <div className="mt-8 flex justify-end border-t border-line pt-6">
            <div className="h-11 w-32 rounded-full shimmer" />
          </div>
        </div>
      </div>
    </>
  );
}
