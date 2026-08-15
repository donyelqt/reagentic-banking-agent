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
  signedAmount: string
  balanceAfter: string
  createdAt: number
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
  reply: string
}

export interface ChatRequest {
  message: string
  history?: string[]
  plan?: Step[]
  approval?: string[]
  jwt?: string
}
