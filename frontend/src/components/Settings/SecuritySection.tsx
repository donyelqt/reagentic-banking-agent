import { useEffect, useRef, useState, type FormEvent } from "react";
import { useMutation } from "@tanstack/react-query";
import { ApiRequestError, changePassword, sessionFromToken, type Session } from "../../api";
import type { UserProfile } from "../../types";

const MIN_LENGTH = 8;
const MAX_LENGTH = 64;
const MAX_BYTES = 72;

type ChangePasswordVars = {
  email: string;
  token: string | null;
  currentPassword: string;
  newPassword: string;
};

type FieldErrors = {
  current?: string;
  next?: string;
  confirm?: string;
  form?: string;
};

type FieldName = "current" | "next" | "confirm";

export default function SecuritySection({
  profile,
  session,
  roleLabel,
  onSignOut,
}: {
  profile: UserProfile;
  session: Session | null;
  roleLabel: string;
  onSignOut: () => void;
}) {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [errors, setErrors] = useState<FieldErrors>({});
  const [saved, setSaved] = useState(false);
  const [visible, setVisible] = useState(false);
  const [focusRequest, setFocusRequest] = useState<{ field: FieldName; nonce: number } | null>(null);

  const currentRef = useRef<HTMLInputElement>(null);
  const nextRef = useRef<HTMLInputElement>(null);
  const confirmRef = useRef<HTMLInputElement>(null);
  const focusNonce = useRef(0);

  const mutation = useMutation({
    mutationFn: ({ currentPassword, newPassword, token }: ChangePasswordVars) =>
      changePassword({ currentPassword, newPassword }, token),
    onSuccess: (_response, variables) => {
      if (!isSameSession(variables)) return;
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setErrors({});
      setFocusRequest(null);
      setSaved(true);
    },
    onError: (error, variables) => {
      if (!isSameSession(variables)) return;
      setSaved(false);
      const nextErrors = mapServerError(error);
      setErrors(nextErrors);
      const field = firstErrorField(nextErrors);
      if (field) requestFocus(field);
    },
  });

  useEffect(() => {
    if (!focusRequest) return;
    if (focusRequest.field === "current") currentRef.current?.focus();
    else if (focusRequest.field === "next") nextRef.current?.focus();
    else confirmRef.current?.focus();
  }, [focusRequest]);

  function requestFocus(field: FieldName) {
    focusNonce.current += 1;
    setFocusRequest({ field, nonce: focusNonce.current });
  }

  function isSameSession(variables: ChangePasswordVars) {
    const currentToken = localStorage.getItem("jwt");
    return (
      currentToken !== null &&
      currentToken === variables.token &&
      sessionFromToken(currentToken)?.email === variables.email
    );
  }

  function edit(field: "current" | "next" | "confirm", value: string) {
    if (mutation.isPending) return;
    setSaved(false);
    mutation.reset();
    setErrors((current) => ({ ...current, [field]: undefined, form: undefined }));
    if (field === "current") setCurrentPassword(value);
    if (field === "next") setNewPassword(value);
    if (field === "confirm") setConfirmPassword(value);
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (mutation.isPending) return;
    setSaved(false);

    const nextErrors: FieldErrors = {};
    if (!currentPassword.trim()) {
      nextErrors.current = "Enter your current password.";
    }
    if (!newPassword.trim()) {
      nextErrors.next = "Enter a new password.";
    } else if (newPassword.length < MIN_LENGTH || newPassword.length > MAX_LENGTH) {
      nextErrors.next = `Use between ${MIN_LENGTH} and ${MAX_LENGTH} characters.`;
    } else if (byteLength(newPassword) > MAX_BYTES) {
      nextErrors.next = "That password is too long. Use 72 bytes or fewer.";
    } else if (newPassword === currentPassword) {
      nextErrors.next = "Choose a password different from your current password.";
    }
    if (!confirmPassword.trim()) {
      nextErrors.confirm = "Confirm your new password.";
    } else if (confirmPassword !== newPassword) {
      nextErrors.confirm = "Passwords don't match.";
    }

    setErrors(nextErrors);
    const field = firstErrorField(nextErrors);
    if (field) {
      requestFocus(field);
      return;
    }

    mutation.mutate({
      email: profile.email,
      token: localStorage.getItem("jwt"),
      currentPassword,
      newPassword,
    });
  }

  return (
    <section id="security" aria-labelledby="security-heading" className="scroll-mt-24">
      <div className="mb-4">
        <h2 id="security-heading" className="text-2xl">Security</h2>
        <p className="mt-1 text-sm text-muted">Change the password you use to sign in, and review this session.</p>
      </div>

      <form onSubmit={submit} aria-busy={mutation.isPending} className="card p-6 md:p-8" noValidate>
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h3 className="text-xl">Change password</h3>
            <p className="mt-1 text-sm text-muted">
              Use {MIN_LENGTH} to {MAX_LENGTH} characters, up to {MAX_BYTES} bytes.
            </p>
          </div>
          <button
            type="button"
            onClick={() => setVisible((value) => !value)}
            aria-pressed={visible}
            className="text-sm font-medium text-muted transition hover:text-ink"
          >
            {visible ? "Hide passwords" : "Show passwords"}
          </button>
        </div>

        <div className="mt-6 grid gap-5">
          <div>
            <label htmlFor="currentPassword" className="label">Current password</label>
            <input
              id="currentPassword"
              ref={currentRef}
              type={visible ? "text" : "password"}
              className="field mt-1.5 disabled:opacity-60"
              value={currentPassword}
              onChange={(event) => edit("current", event.target.value)}
              autoComplete="current-password"
              disabled={mutation.isPending}
              aria-invalid={errors.current ? true : undefined}
              aria-describedby={errors.current ? "currentPassword-error" : undefined}
            />
            {errors.current && (
              <p id="currentPassword-error" className="mt-2 text-sm text-neg">{errors.current}</p>
            )}
          </div>

          <div>
            <label htmlFor="newPassword" className="label">New password</label>
            <input
              id="newPassword"
              ref={nextRef}
              type={visible ? "text" : "password"}
              className="field mt-1.5 disabled:opacity-60"
              value={newPassword}
              onChange={(event) => edit("next", event.target.value)}
              autoComplete="new-password"
              disabled={mutation.isPending}
              aria-invalid={errors.next ? true : undefined}
              aria-describedby={errors.next ? "newPassword-error" : "newPassword-hint"}
            />
            {errors.next ? (
              <p id="newPassword-error" className="mt-2 text-sm text-neg">{errors.next}</p>
            ) : (
              <p id="newPassword-hint" className="mt-2 text-xs text-muted">
                Not your email, name, or the demo password.
              </p>
            )}
          </div>

          <div>
            <label htmlFor="confirmPassword" className="label">Confirm new password</label>
            <input
              id="confirmPassword"
              ref={confirmRef}
              type={visible ? "text" : "password"}
              className="field mt-1.5 disabled:opacity-60"
              value={confirmPassword}
              onChange={(event) => edit("confirm", event.target.value)}
              autoComplete="new-password"
              disabled={mutation.isPending}
              aria-invalid={errors.confirm ? true : undefined}
              aria-describedby={errors.confirm ? "confirmPassword-error" : undefined}
            />
            {errors.confirm && (
              <p id="confirmPassword-error" className="mt-2 text-sm text-neg">{errors.confirm}</p>
            )}
          </div>
        </div>

        <div className="mt-8 flex flex-wrap items-center justify-between gap-4 border-t border-line pt-6">
          <div>
            <div role="status" aria-live="polite">
              {saved && <p className="text-sm text-pos">Password updated.</p>}
            </div>
            {errors.form && <p role="alert" className="text-sm text-neg">{errors.form}</p>}
          </div>

          <button
            type="submit"
            disabled={mutation.isPending}
            className="btn btn-accent active:scale-[.98] disabled:cursor-not-allowed disabled:opacity-50"
          >
            {mutation.isPending ? "Updating..." : "Update password"}
          </button>
        </div>
      </form>

      <div className="card mt-6 p-6 md:p-8">
        <h3 className="text-xl">This session</h3>
        <p className="mt-1 text-sm text-muted">How you're signed in on this device.</p>

        <dl className="mt-6 grid gap-5 sm:grid-cols-2">
          <div>
            <dt className="label">Signed in as</dt>
            <dd className="mt-1 truncate text-sm" title={session?.email ?? profile.email}>
              {session?.email ?? profile.email}
            </dd>
          </div>
          <div>
            <dt className="label">Role</dt>
            <dd className="mt-1 text-sm">{roleLabel}</dd>
          </div>
          <div>
            <dt className="label">Session started</dt>
            <dd className="mt-1 text-sm">{formatMoment(session?.issuedAt)}</dd>
          </div>
          <div>
            <dt className="label">Session expires</dt>
            <dd className="mt-1 text-sm">{formatMoment(session?.expiresAt)}</dd>
          </div>
        </dl>

        <p className="mt-6 text-xs text-muted">
          Changing your password does not end other sessions. They stay signed in until their token expires.
        </p>

        <div className="mt-6 border-t border-line pt-6">
          <button
            type="button"
            onClick={onSignOut}
            className="btn btn-ghost active:scale-[.98]"
          >
            Sign out
          </button>
        </div>
      </div>
    </section>
  );
}

function byteLength(value: string) {
  return new TextEncoder().encode(value).length;
}

function mapServerError(error: unknown): FieldErrors {
  if (!(error instanceof ApiRequestError)) {
    return { form: "We couldn't update your password. Try again in a moment." };
  }

  if (error.code === "INVALID_CURRENT_PASSWORD") {
    return { current: "That isn't your current password." };
  }
  if (error.code === "PASSWORD_REUSE") {
    return { next: "Choose a password different from your current password." };
  }
  if (error.code === "PASSWORD_TOO_LONG") {
    return { next: "That password is too long. Use 72 bytes or fewer." };
  }
  if (error.code === "VALIDATION") {
    const required = error.message.includes("required");
    if (error.message.startsWith("currentPassword")) {
      return { current: required ? "Enter your current password." : "We couldn't verify that current password." };
    }
    return {
      next: required ? "Enter a new password." : `Use between ${MIN_LENGTH} and ${MAX_LENGTH} characters.`,
    };
  }
  return { form: "We couldn't update your password. Try again in a moment." };
}

function firstErrorField(errors: FieldErrors): FieldName | null {
  if (errors.current) return "current";
  if (errors.next) return "next";
  if (errors.confirm) return "confirm";
  return null;
}

function formatMoment(moment: number | undefined) {
  if (!moment) return "Unknown";
  return new Date(moment).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" });
}
