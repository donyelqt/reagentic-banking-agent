export interface AccountView {
  accountId: string
  type: string
  balance: string
}

export interface LedgerEntry {
  entryId: number
  accountId: string
  paymentId: string | null
  type: string
  description: string | null
  signedAmount: string
  balanceAfter: string
  createdAt: number
}

export interface CategorySpend {
  category: string
  total: number
  count: number
}

export interface Step {
  stepId: string
  worker: string
  tool: string
  args: Record<string, any>
  dependsOn: string[]
  confirmationRequired: boolean
  idempotencyKey: string | null
}

export interface StepResult {
  stepId: string
  ok: boolean
  data: any
  error: string | null
}

export interface AgentResponse {
  plan: Step[]
  results: StepResult[]
  pendingSteps: Step[]
  approvalId?: string | null
  reply: string
}

export interface ChatRequest {
  message?: string
  history?: string[]
  plan?: Step[]
  approval?: string[]
  jwt?: string
  approvalId?: string
}

export interface ReconcileEvidence {
  entryId: string
  type: string
  signedAmount: string
  balanceAfter: string
  paymentId: string
}

export interface ReconcileResult {
  accountId: string
  balance: string
  ledgerSum: string
  balanced: boolean
  delta?: string
  direction?: string
  missingAmount?: string
  lastEntryId?: string
  lastPaymentId?: string
  lastBalanceAfter?: string
  diagnosis?: string
  evidenceCount?: number
  evidence?: ReconcileEvidence[]
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
}

export interface LoginResponse {
  token?: string;
  data?: { token?: string };
}

export interface TransferResponse {
  success?: boolean;
  data?: { status?: string; reason?: string; paymentId?: string };
  status?: string;
  reason?: string;
}

export interface ClassifySummaryResponse {
  summary?: Array<{ category: string; total: string | number; count?: number }>;
  data?: {
    summary?: Array<{ category: string; total: string | number; count?: number }>;
  };
}

export interface ClassifyTransactionsResponse {
  transactions?: Array<{ category?: string }>;
  data?: {
    transactions?: Array<{ category?: string }>;
  };
}