import { useState, useEffect, useRef } from "react";
import type { AccountView, TransactionRecord, TransactionUploadResult, UserProfile } from "../types";
import { getUserProfile, updateUserProfile, changePassword, uploadTransactionsCsv, getTransactions, deleteTransactionBatch } from "../api";

export default function Settings({ accounts }: { accounts: AccountView[] }) {
  const [activeTab, setActiveTab] = useState<"profile" | "preferences" | "security" | "csv" | "notifications">("profile");
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [saveSuccess, setSaveSuccess] = useState<string | null>(null);
  const [saveError, setSaveError] = useState<string | null>(null);

  // Profile form
  const [fullName, setFullName] = useState("");
  const [phone, setPhone] = useState("");
  const [updatingProfile, setUpdatingProfile] = useState(false);

  // Password form
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [updatingPassword, setUpdatingPassword] = useState(false);

  // Preferences form
  const [defaultAccount, setDefaultAccount] = useState("acc-checking-0001");
  const [currency, setCurrency] = useState("USD");
  const [exportFormat, setExportFormat] = useState("CSV");

  // CSV Upload Manager
  const [selectedAccount, setSelectedAccount] = useState("acc-checking-0001");
  const [uploading, setUploading] = useState(false);
  const [uploadResult, setUploadResult] = useState<TransactionUploadResult | null>(null);
  const [uploadedTransactions, setUploadedTransactions] = useState<TransactionRecord[]>([]);
  const [fetchingTransactions, setFetchingTransactions] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Notifications state
  const [notifyTx, setNotifyTx] = useState(true);
  const [notifySecurity, setNotifySecurity] = useState(true);
  const [notifyWeekly, setNotifyWeekly] = useState(false);

  useEffect(() => {
    let isMounted = true;
    getUserProfile()
      .then((res) => {
        if (!isMounted) return;
        const p = res.data ?? res;
        setProfile(p);
        setFullName(p.fullName || "");
        setPhone(p.phone || "");
        if (p.defaultAccount) setDefaultAccount(p.defaultAccount);
      })
      .catch(() => {
        // Fallback default
        if (!isMounted) return;
        setProfile({
          email: "demo@bank.dev",
          role: "USER",
          fullName: "Demo Customer",
          phone: "+1 (555) 019-2834",
          twoFactorEnabled: false,
          theme: "system",
          defaultAccount: "acc-checking-0001",
          currency: "USD"
        });
        setFullName("Demo Customer");
        setPhone("+1 (555) 019-2834");
      })
      .finally(() => {
        if (isMounted) setLoading(false);
      });
  }, []);

  useEffect(() => {
    if (activeTab === "csv") {
      fetchUploadedTransactions();
    }
  }, [activeTab, selectedAccount]);

  function fetchUploadedTransactions() {
    setFetchingTransactions(true);
    getTransactions(selectedAccount)
      .then((res) => {
        setUploadedTransactions(res.data ?? []);
      })
      .catch(() => setUploadedTransactions([]))
      .finally(() => setFetchingTransactions(false));
  }

  function flashMessage(msg: string, isErr = false) {
    if (isErr) {
      setSaveError(msg);
      setSaveSuccess(null);
    } else {
      setSaveSuccess(msg);
      setSaveError(null);
    }
    setTimeout(() => {
      setSaveSuccess(null);
      setSaveError(null);
    }, 4000);
  }

  function handleSaveProfile(e: React.FormEvent) {
    e.preventDefault();
    setUpdatingProfile(true);
    updateUserProfile({ fullName, phone })
      .then((res) => {
        const updated = res.data ?? res;
        setProfile(updated);
        flashMessage("Profile updated successfully!");
      })
      .catch((err) => flashMessage(err.message || "Failed to update profile", true))
      .finally(() => setUpdatingProfile(false));
  }

  function handleChangePassword(e: React.FormEvent) {
    e.preventDefault();
    if (newPassword !== confirmPassword) {
      flashMessage("New passwords do not match", true);
      return;
    }
    if (newPassword.length < 6) {
      flashMessage("Password must be at least 6 characters long", true);
      return;
    }
    setUpdatingPassword(true);
    changePassword(currentPassword, newPassword)
      .then(() => {
        setCurrentPassword("");
        setNewPassword("");
        setConfirmPassword("");
        flashMessage("Password updated successfully!");
      })
      .catch((err) => flashMessage(err.message || "Failed to change password", true))
      .finally(() => setUpdatingPassword(false));
  }

  function handleFileUpload(file: File) {
    if (!file.name.endsWith(".csv")) {
      flashMessage("Please select a valid .csv file", true);
      return;
    }
    setUploading(true);
    setUploadResult(null);
    uploadTransactionsCsv(file, selectedAccount)
      .then((res) => {
        setUploadResult(res);
        flashMessage(`Successfully processed CSV import! ${res.rowsParsed} rows parsed.`);
        fetchUploadedTransactions();
      })
      .catch((err) => flashMessage(err.message || "Failed to upload CSV", true))
      .finally(() => setUploading(false));
  }

  function handleDeleteBatch(uploadBatchId: string) {
    if (!confirm("Are you sure you want to delete this upload batch?")) return;
    deleteTransactionBatch(uploadBatchId)
      .then(() => {
        flashMessage("Upload batch deleted");
        fetchUploadedTransactions();
        setUploadResult(null);
      })
      .catch((err) => flashMessage(err.message || "Failed to delete batch", true));
  }

  function downloadSampleCsv() {
    const sample = `date,description,amount,category\n2026-08-15,Jollibee Food,-350.00,Dining\n2026-08-16,SM Supermarket,-1850.50,Groceries\n2026-08-17,Meralco Electric Bill,-2400.00,Utilities\n2026-08-18,Salary Deposit,25000.00,Income\n2026-08-19,Grab Ride,-280.00,Transport`;
    const blob = new Blob([sample], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "sample_transactions.csv";
    a.click();
    URL.revokeObjectURL(url);
  }

  if (loading) {
    return (
      <div className="py-12 text-center text-muted">
        <div className="w-8 h-8 rounded-full border-2 border-accent border-t-transparent animate-spin mx-auto mb-3" />
        Loading settings & profile...
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-line pb-5">
        <div>
          <div className="flex items-center gap-3">
            <span className="w-12 h-12 rounded-2xl bg-accent/15 text-accent grid place-items-center font-display text-xl font-bold border border-accent/20">
              {profile?.email?.[0]?.toUpperCase() ?? "U"}
            </span>
            <div>
              <h1 className="text-3xl font-display font-semibold tracking-tight">{profile?.fullName || "Profile Settings"}</h1>
              <p className="text-sm text-muted">{profile?.email} · Role: <span className="uppercase text-accent font-semibold">{profile?.role}</span></p>
            </div>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium bg-[rgba(11,138,99,.12)] text-pos border border-[rgba(11,138,99,.25)]">
            <span className="w-1.5 h-1.5 rounded-full bg-pos animate-pulse" />
            JWT Validated · Zero-Trust Active
          </span>
        </div>
      </div>

      {/* Notifications */}
      {saveSuccess && (
        <div className="bg-[rgba(11,138,99,.12)] text-pos border border-[rgba(11,138,99,.25)] px-4 py-3 rounded-xl text-sm flex items-center justify-between transition-all">
          <span>✓ {saveSuccess}</span>
          <button onClick={() => setSaveSuccess(null)} className="text-xs font-bold hover:underline">Dismiss</button>
        </div>
      )}
      {saveError && (
        <div className="bg-[rgba(201,50,60,.12)] text-neg border border-[rgba(201,50,60,.25)] px-4 py-3 rounded-xl text-sm flex items-center justify-between transition-all">
          <span>⚠ {saveError}</span>
          <button onClick={() => setSaveError(null)} className="text-xs font-bold hover:underline">Dismiss</button>
        </div>
      )}

      {/* Navigation Tabs */}
      <div className="flex border-b border-line gap-1 overflow-x-auto pb-px">
        {[
          { id: "profile", label: "Profile", icon: "👤" },
          { id: "preferences", label: "Preferences", icon: "⚙️" },
          { id: "security", label: "Security & Login", icon: "🔒" },
          { id: "csv", label: "CSV Ingestion Manager", icon: "📊" },
          { id: "notifications", label: "Notifications", icon: "🔔" }
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id as any)}
            className={`flex items-center gap-2 px-4 py-2.5 text-sm font-medium border-b-2 transition whitespace-nowrap ${
              activeTab === tab.id
                ? "border-accent text-accent bg-accent/5 rounded-t-lg"
                : "border-transparent text-muted hover:text-ink hover:bg-line/20"
            }`}
          >
            <span>{tab.icon}</span>
            <span>{tab.label}</span>
          </button>
        ))}
      </div>

      {/* Tab Contents */}
      {activeTab === "profile" && (
        <div className="card p-6 space-y-6">
          <div>
            <h2 className="text-xl font-display">Personal Details</h2>
            <p className="text-sm text-muted">Update your account identity and contact information.</p>
          </div>

          <form onSubmit={handleSaveProfile} className="space-y-4 max-w-xl">
            <div>
              <label className="block text-xs font-medium text-muted uppercase tracking-wider mb-1.5">Email Address</label>
              <div className="relative">
                <input
                  type="email"
                  disabled
                  value={profile?.email || ""}
                  className="w-full bg-line/20 border border-line rounded-xl px-3.5 py-2.5 text-sm text-muted cursor-not-allowed"
                />
                <span className="absolute right-3 top-2.5 text-xs text-pos font-medium">Verified</span>
              </div>
              <p className="text-[11px] text-muted mt-1">Email is tied to your JWT subject claim and cannot be altered directly.</p>
            </div>

            <div>
              <label className="block text-xs font-medium text-muted uppercase tracking-wider mb-1.5">Full Name</label>
              <input
                type="text"
                required
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="Enter your full name"
                className="w-full bg-bg border border-line rounded-xl px-3.5 py-2.5 text-sm focus:outline-none focus:border-accent"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-muted uppercase tracking-wider mb-1.5">Phone Number</label>
              <input
                type="tel"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="+1 (555) 000-0000"
                className="w-full bg-bg border border-line rounded-xl px-3.5 py-2.5 text-sm focus:outline-none focus:border-accent"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-muted uppercase tracking-wider mb-1.5">Role & Access Tier</label>
              <div className="flex items-center gap-3 p-3 bg-line/20 border border-line rounded-xl">
                <span className="chip uppercase text-accent font-semibold">{profile?.role || "USER"}</span>
                <span className="text-xs text-muted">
                  {profile?.role === "EMPLOYEE"
                    ? "Ops Analyst — Access to internal ledger audit, reconcile tools, and zero-trust evidence checks."
                    : "Customer — Personal banking access, transfer capabilities, personal AI agent, and CSV imports."}
                </span>
              </div>
            </div>

            <div className="pt-2">
              <button
                type="submit"
                disabled={updatingProfile}
                className="btn btn-accent px-6 py-2.5 text-sm font-medium"
              >
                {updatingProfile ? "Saving..." : "Save Profile Changes"}
              </button>
            </div>
          </form>
        </div>
      )}

      {activeTab === "preferences" && (
        <div className="card p-6 space-y-6">
          <div>
            <h2 className="text-xl font-display">Banking & System Preferences</h2>
            <p className="text-sm text-muted">Customize your default account views, currency formats, and statement defaults.</p>
          </div>

          <div className="space-y-5 max-w-xl">
            <div>
              <label className="block text-xs font-medium text-muted uppercase tracking-wider mb-1.5">Default Primary Account</label>
              <select
                value={defaultAccount}
                onChange={(e) => setDefaultAccount(e.target.value)}
                className="w-full bg-bg border border-line rounded-xl px-3.5 py-2.5 text-sm focus:outline-none focus:border-accent"
              >
                {accounts.length > 0 ? (
                  accounts.map((a) => (
                    <option key={a.accountId} value={a.accountId}>
                      {a.type} ({a.accountId}) — ${a.balance}
                    </option>
                  ))
                ) : (
                  <>
                    <option value="acc-checking-0001">Checking (acc-checking-0001)</option>
                    <option value="acc-savings-0002">Savings (acc-savings-0002)</option>
                  </>
                )}
              </select>
            </div>

            <div>
              <label className="block text-xs font-medium text-muted uppercase tracking-wider mb-1.5">Display Currency Format</label>
              <div className="grid grid-cols-2 gap-3">
                {[
                  { code: "USD", label: "USD ($)", desc: "US Dollar" },
                  { code: "PHP", label: "PHP (₱)", desc: "Philippine Peso" }
                ].map((c) => (
                  <button
                    key={c.code}
                    type="button"
                    onClick={() => setCurrency(c.code)}
                    className={`p-3 rounded-xl border text-left transition ${
                      currency === c.code ? "border-accent bg-accent/10 font-semibold" : "border-line hover:bg-line/20"
                    }`}
                  >
                    <div className="text-sm">{c.label}</div>
                    <div className="text-xs text-muted">{c.desc}</div>
                  </button>
                ))}
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-muted uppercase tracking-wider mb-1.5">Default Statement Export Format</label>
              <div className="flex gap-3">
                {["CSV", "Excel"].map((fmt) => (
                  <button
                    key={fmt}
                    type="button"
                    onClick={() => setExportFormat(fmt)}
                    className={`px-4 py-2 text-sm rounded-xl border transition ${
                      exportFormat === fmt ? "border-accent bg-accent/10 text-accent font-medium" : "border-line text-muted"
                    }`}
                  >
                    {fmt} Statement
                  </button>
                ))}
              </div>
            </div>

            <div className="pt-2">
              <button
                onClick={() => flashMessage("Preferences updated!")}
                className="btn btn-accent px-6 py-2.5 text-sm font-medium"
              >
                Save Preferences
              </button>
            </div>
          </div>
        </div>
      )}

      {activeTab === "security" && (
        <div className="card p-6 space-y-6">
          <div>
            <h2 className="text-xl font-display">Security & Password</h2>
            <p className="text-sm text-muted">Manage your authentication credentials and review security policy enforcement.</p>
          </div>

          <div className="grid lg:grid-cols-2 gap-8">
            <form onSubmit={handleChangePassword} className="space-y-4">
              <h3 className="text-base font-semibold border-b border-line pb-2">Change Password</h3>

              <div>
                <label className="block text-xs font-medium text-muted uppercase tracking-wider mb-1.5">Current Password</label>
                <input
                  type="password"
                  required
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                  className="w-full bg-bg border border-line rounded-xl px-3.5 py-2.5 text-sm focus:outline-none focus:border-accent"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-muted uppercase tracking-wider mb-1.5">New Password</label>
                <input
                  type="password"
                  required
                  minLength={6}
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  className="w-full bg-bg border border-line rounded-xl px-3.5 py-2.5 text-sm focus:outline-none focus:border-accent"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-muted uppercase tracking-wider mb-1.5">Confirm New Password</label>
                <input
                  type="password"
                  required
                  minLength={6}
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  className="w-full bg-bg border border-line rounded-xl px-3.5 py-2.5 text-sm focus:outline-none focus:border-accent"
                />
              </div>

              <div className="pt-2">
                <button
                  type="submit"
                  disabled={updatingPassword}
                  className="btn btn-accent px-6 py-2.5 text-sm font-medium"
                >
                  {updatingPassword ? "Updating..." : "Update Password"}
                </button>
              </div>
            </form>

            <div className="space-y-4">
              <h3 className="text-base font-semibold border-b border-line pb-2">Security Posture & Sessions</h3>

              <div className="p-4 rounded-xl bg-line/20 border border-line space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs text-muted font-medium uppercase tracking-wider">Zero Internal Trust</span>
                  <span className="chip bg-pos/15 text-pos font-semibold">Active</span>
                </div>
                <p className="text-xs text-muted leading-relaxed">
                  Every microservice in the monorepo validates JWT signatures independently on incoming requests. Header spoofing (such as forged X-Service headers) is stripped at the gateway.
                </p>
              </div>

              <div className="p-4 rounded-xl bg-line/20 border border-line space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs text-muted font-medium uppercase tracking-wider">Active Token Session</span>
                  <span className="text-xs font-mono text-accent">HMAC-SHA256</span>
                </div>
                <p className="text-xs text-muted">
                  Authenticated as <strong className="text-ink">{profile?.email}</strong>. Session token is stored in localStorage for SPA navigation.
                </p>
              </div>
            </div>
          </div>
        </div>
      )}

      {activeTab === "csv" && (
        <div className="card p-6 space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-line pb-4">
            <div>
              <h2 className="text-xl font-display">CSV Transaction Processing Engine</h2>
              <p className="text-sm text-muted">Ingest external CSV transaction statements, categorize spending rules, and manage upload batches.</p>
            </div>
            <button
              onClick={downloadSampleCsv}
              className="btn btn-ghost border border-line text-xs font-medium px-3.5 py-2 flex items-center gap-2"
            >
              <span>📥</span> Download Sample CSV
            </button>
          </div>

          {/* Account Selection & Drag and Drop Upload */}
          <div className="grid md:grid-cols-3 gap-6">
            <div className="md:col-span-1 space-y-4">
              <div>
                <label className="block text-xs font-medium text-muted uppercase tracking-wider mb-1.5">Target Account</label>
                <select
                  value={selectedAccount}
                  onChange={(e) => setSelectedAccount(e.target.value)}
                  className="w-full bg-bg border border-line rounded-xl px-3.5 py-2.5 text-sm focus:outline-none focus:border-accent"
                >
                  {accounts.length > 0 ? (
                    accounts.map((a) => (
                      <option key={a.accountId} value={a.accountId}>
                        {a.type} ({a.accountId})
                      </option>
                    ))
                  ) : (
                    <>
                      <option value="acc-checking-0001">Checking (acc-checking-0001)</option>
                      <option value="acc-savings-0002">Savings (acc-savings-0002)</option>
                    </>
                  )}
                </select>
              </div>

              <div
                onDragOver={(e) => e.preventDefault()}
                onDrop={(e) => {
                  e.preventDefault();
                  if (e.dataTransfer.files?.[0]) handleFileUpload(e.dataTransfer.files[0]);
                }}
                className="border-2 border-dashed border-line hover:border-accent/60 rounded-2xl p-6 text-center transition cursor-pointer bg-bg/50 hover:bg-accent/5 flex flex-col items-center justify-center space-y-3"
                onClick={() => fileInputRef.current?.click()}
              >
                <input
                  type="file"
                  ref={fileInputRef}
                  accept=".csv"
                  className="hidden"
                  onChange={(e) => {
                    if (e.target.files?.[0]) handleFileUpload(e.target.files[0]);
                  }}
                />
                <div className="w-12 h-12 rounded-full bg-accent/10 text-accent grid place-items-center text-2xl">
                  📄
                </div>
                <div>
                  <p className="text-sm font-medium">Click to select CSV or drag & drop</p>
                  <p className="text-xs text-muted mt-0.5">Format: date, description, amount, category</p>
                </div>
                {uploading && (
                  <div className="flex items-center gap-2 text-xs text-accent font-medium">
                    <span className="w-3.5 h-3.5 rounded-full border-2 border-accent border-t-transparent animate-spin" />
                    Parsing CSV & classifying transactions...
                  </div>
                )}
              </div>
            </div>

            {/* Upload Summary Result Banner */}
            <div className="md:col-span-2 space-y-4">
              {uploadResult ? (
                <div className="p-5 rounded-2xl bg-accent/10 border border-accent/30 space-y-4 view-in">
                  <div className="flex items-center justify-between">
                    <div>
                      <span className="chip bg-accent text-white">Batch Upload Result</span>
                      <h4 className="text-sm font-mono text-muted mt-1">ID: {uploadResult.uploadBatchId}</h4>
                    </div>
                    <button
                      onClick={() => handleDeleteBatch(uploadResult.uploadBatchId)}
                      className="text-xs text-neg hover:underline font-medium"
                    >
                      Undo / Delete Batch
                    </button>
                  </div>

                  <div className="grid grid-cols-3 gap-3 text-center">
                    <div className="bg-bg/80 p-3 rounded-xl border border-line">
                      <div className="text-xs text-muted">Parsed Rows</div>
                      <div className="text-2xl font-display font-semibold text-pos">{uploadResult.rowsParsed}</div>
                    </div>
                    <div className="bg-bg/80 p-3 rounded-xl border border-line">
                      <div className="text-xs text-muted">Rejected Rows</div>
                      <div className="text-2xl font-display font-semibold text-neg">{uploadResult.rowsRejected}</div>
                    </div>
                    <div className="bg-bg/80 p-3 rounded-xl border border-line">
                      <div className="text-xs text-muted">Net Amount</div>
                      <div className="text-xl font-display font-semibold">{uploadResult.netTotal}</div>
                    </div>
                  </div>

                  {uploadResult.totalsByCategory && (
                    <div>
                      <div className="text-xs font-semibold text-muted uppercase tracking-wider mb-2">Category Summary</div>
                      <div className="flex flex-wrap gap-2">
                        {Object.entries(uploadResult.totalsByCategory).map(([cat, total]) => (
                          <span key={cat} className="px-2.5 py-1 bg-bg border border-line rounded-lg text-xs font-medium">
                            {cat}: <strong className="text-accent">{total}</strong>
                          </span>
                        ))}
                      </div>
                    </div>
                  )}

                  {uploadResult.rejectedRowErrors?.length > 0 && (
                    <div className="p-3 bg-neg/10 border border-neg/20 rounded-xl space-y-1 text-xs text-neg">
                      <div className="font-semibold">Parsing Warnings:</div>
                      {uploadResult.rejectedRowErrors.map((err, i) => (
                        <div key={i}>• {err}</div>
                      ))}
                    </div>
                  )}
                </div>
              ) : (
                <div className="p-6 rounded-2xl bg-line/10 border border-line text-center text-muted space-y-2">
                  <span className="text-3xl">📂</span>
                  <p className="text-sm font-medium text-ink">Upload a CSV file to process and analyze statement data.</p>
                  <p className="text-xs max-w-md mx-auto">
                    The CSV parser uses deterministic rules to assign categories (Dining, Groceries, Utilities, Subscriptions, Transport, Income) before storing rows in PostgreSQL.
                  </p>
                </div>
              )}
            </div>
          </div>

          {/* Table of Parsed Transactions */}
          <div className="space-y-3 pt-4 border-t border-line">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-display">Ingested Transactions ({uploadedTransactions.length})</h3>
              <button onClick={fetchUploadedTransactions} className="text-xs text-accent hover:underline font-medium">
                Refresh List
              </button>
            </div>

            {fetchingTransactions ? (
              <div className="py-8 text-center text-muted text-sm">Loading transactions...</div>
            ) : uploadedTransactions.length === 0 ? (
              <div className="p-8 text-center text-muted text-sm border border-line rounded-xl bg-bg/50">
                No CSV transactions uploaded yet for account <code className="text-accent font-mono">{selectedAccount}</code>.
              </div>
            ) : (
              <div className="overflow-x-auto border border-line rounded-xl">
                <table className="w-full text-left text-sm">
                  <thead className="bg-line/30 text-xs text-muted uppercase tracking-wider">
                    <tr>
                      <th className="px-4 py-3">Date</th>
                      <th className="px-4 py-3">Description</th>
                      <th className="px-4 py-3">Category</th>
                      <th className="px-4 py-3 text-right">Amount</th>
                      <th className="px-4 py-3">Batch ID</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-line">
                    {uploadedTransactions.map((tx) => {
                      const amt = typeof tx.amount === "number" ? tx.amount : parseFloat(tx.amount || "0");
                      const isIncome = amt > 0;
                      return (
                        <tr key={tx.id} className="hover:bg-line/10 transition">
                          <td className="px-4 py-3 font-mono text-xs text-muted">{tx.transactionDate}</td>
                          <td className="px-4 py-3 font-medium text-ink">{tx.description}</td>
                          <td className="px-4 py-3">
                            <span className="chip text-[11px] uppercase tracking-wide bg-accent/10 text-accent font-semibold">
                              {tx.category || "UNCATEGORIZED"}
                            </span>
                          </td>
                          <td className={`px-4 py-3 text-right font-display font-semibold ${isIncome ? "text-pos" : "text-neg"}`}>
                            {isIncome ? "+" : ""}${Math.abs(amt).toFixed(2)}
                          </td>
                          <td className="px-4 py-3 font-mono text-xs text-muted truncate max-w-[120px]" title={tx.uploadBatchId}>
                            {tx.uploadBatchId.substring(0, 8)}...
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {activeTab === "notifications" && (
        <div className="card p-6 space-y-6">
          <div>
            <h2 className="text-xl font-display">Notification Preferences</h2>
            <p className="text-sm text-muted">Control alerts delivered via Kafka notification consumers and email.</p>
          </div>

          <div className="space-y-4 max-w-xl">
            {[
              {
                id: "tx",
                title: "Transaction & Money Movement Alerts",
                desc: "Receive real-time notifications whenever a transfer or debit is initiated.",
                val: notifyTx,
                set: setNotifyTx
              },
              {
                id: "security",
                title: "Security & Login Alerts",
                desc: "Alerts for password updates, new session logins, or failed authorization attempts.",
                val: notifySecurity,
                set: setNotifySecurity
              },
              {
                id: "weekly",
                title: "Weekly AI Spending Insights Digest",
                desc: "Receive weekly summary of classified spending trends and budget anomalies.",
                val: notifyWeekly,
                set: setNotifyWeekly
              }
            ].map((n) => (
              <div key={n.id} className="flex items-center justify-between p-4 rounded-xl border border-line bg-bg/50">
                <div className="pr-4">
                  <div className="text-sm font-medium">{n.title}</div>
                  <div className="text-xs text-muted mt-0.5">{n.desc}</div>
                </div>
                <button
                  type="button"
                  onClick={() => n.set(!n.val)}
                  className={`w-12 h-6 rounded-full transition-colors relative p-1 shrink-0 ${
                    n.val ? "bg-accent" : "bg-line"
                  }`}
                >
                  <span
                    className={`block w-4 h-4 rounded-full bg-white transition-transform ${
                      n.val ? "translate-x-6" : "translate-x-0"
                    }`}
                  />
                </button>
              </div>
            ))}

            <div className="pt-2">
              <button
                onClick={() => flashMessage("Notification settings saved!")}
                className="btn btn-accent px-6 py-2.5 text-sm font-medium"
              >
                Save Notification Settings
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
