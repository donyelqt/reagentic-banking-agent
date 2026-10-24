import { useEffect, useState, type FormEvent } from "react";
import { useMutation } from "@tanstack/react-query";
import { updateProfile } from "../../api";
import { qk, queryClient } from "../../lib/queries";
import type { UserProfile } from "../../types";

export default function ProfileSection({ profile }: { profile: UserProfile }) {
  const [form, setForm] = useState({
    fullName: profile.fullName ?? "",
    phone: profile.phone ?? "",
  });
  const [saved, setSaved] = useState(false);

  const mutation = useMutation({
    mutationFn: updateProfile,
    onSuccess: (response) => {
      queryClient.setQueryData(qk.profile, response.data);
      setForm({
        fullName: response.data.fullName ?? "",
        phone: response.data.phone ?? "",
      });
      setSaved(true);
    },
  });

  useEffect(() => {
    if (!saved) return;
    const timeout = window.setTimeout(() => setSaved(false), 4000);
    return () => window.clearTimeout(timeout);
  }, [saved]);

  const fullName = form.fullName.trim();
  const phone = form.phone.trim();
  const dirty = fullName !== (profile.fullName ?? "") || phone !== (profile.phone ?? "");

  function update(field: "fullName" | "phone", value: string) {
    setSaved(false);
    mutation.reset();
    setForm((current) => ({ ...current, [field]: value }));
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!dirty || mutation.isPending) return;
    mutation.mutate({ fullName: fullName || null, phone: phone || null });
  }

  return (
    <section id="profile" aria-labelledby="profile-heading" className="scroll-mt-24">
      <div className="mb-4">
        <h2 id="profile-heading" className="text-2xl">Profile</h2>
        <p className="mt-1 text-sm text-muted">Keep the name and phone number on your account up to date.</p>
      </div>

      <form onSubmit={submit} aria-busy={mutation.isPending} className="card p-6 md:p-8">
        <div className="grid gap-5 sm:grid-cols-2">
          <div className="sm:col-span-2">
            <label htmlFor="fullName" className="label">Full name</label>
            <input
              id="fullName"
              className="field mt-1.5"
              value={form.fullName}
              onChange={(event) => update("fullName", event.target.value)}
              autoComplete="name"
              maxLength={120}
              placeholder="Add your full name"
            />
            <p className="mt-2 text-xs text-muted">Optional. Used to address you in the app.</p>
          </div>

          <div>
            <label htmlFor="phone" className="label">Phone</label>
            <input
              id="phone"
              className="field mt-1.5"
              type="tel"
              inputMode="tel"
              value={form.phone}
              onChange={(event) => update("phone", event.target.value)}
              autoComplete="tel"
              maxLength={32}
              placeholder="+1 (312) 555-0147"
            />
            <p className="mt-2 text-xs text-muted">Optional. Include a country code.</p>
          </div>

          <div>
            <span className="label">Email</span>
            <div className="mt-1.5 flex min-h-[52px] items-center justify-between gap-3 rounded-[14px] border border-line bg-line/25 px-4 py-3">
              <span className="truncate text-sm">{profile.email}</span>
              <span className="shrink-0 text-[10px] uppercase tracking-[.12em] text-muted">Read-only</span>
            </div>
          </div>
        </div>

        <div className="mt-8 flex flex-wrap items-center justify-between gap-4 border-t border-line pt-6">
          <div aria-live="polite">
            {mutation.isError && (
              <p role="alert" className="text-sm text-neg">
                {mutation.error.message || "We couldn't save your profile. Try again."}
              </p>
            )}
            {saved && !mutation.isError && <p className="text-sm text-pos">Profile saved.</p>}
          </div>

          <button
            type="submit"
            disabled={!dirty || mutation.isPending}
            className="btn btn-accent active:scale-[.98] disabled:cursor-not-allowed disabled:opacity-50"
          >
            {mutation.isPending ? "Saving..." : "Save changes"}
          </button>
        </div>
      </form>
    </section>
  );
}
