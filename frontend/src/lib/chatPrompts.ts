const USER_ACTIONS = ["What's my balance?", "Show my transactions", "Analyze my spending", "Transfer $50 to savings"]
const OPS_ACTIONS = ["Reconcile checking", "Reconcile savings"]

import type { CategorySpend } from '../types'

export function isCapabilityQuestion(text: string): boolean {
  const t = text.trim().toLowerCase()
  return /what can (you|i) do|what do you do|show me what you can do|capabilit|commands|can you help|\bhelp\b/.test(t)
}

export function isAnalyzeQuestion(text: string): boolean {
  const t = text.trim().toLowerCase()
  return /analy|spending|breakdown|where does my money|category/.test(t)
}

function categoryLabel(category: string): string {
  return category.charAt(0).toUpperCase() + category.slice(1).toLowerCase()
}

function money(v: number): string {
  return '$' + v.toLocaleString('en-US', { maximumFractionDigits: 2 })
}

export function analyzeReply(summary: CategorySpend[]): string {
  if (summary.length === 0) {
    return "I couldn't find categorized spending on your accounts yet - move some money first and I'll classify it."
  }
  const total = summary.reduce((a, s) => a + s.total, 0)
  const top = summary.slice(0, 3)
  const lines = top.map((s) => {
    const pct = total > 0 ? Math.round((s.total / total) * 100) : 0
    return `\u2022 ${categoryLabel(s.category)} - ${money(s.total)} (${pct}% of spend)`
  })
  return (
    "Here's your spending picture, straight from the ledger:\n\n" +
    lines.join('\n') +
    `\n\n${top[0] ? categoryLabel(top[0].category) : 'Spending'} is your biggest bucket. Want to act on it? I'll prepare a transfer for your approval.`
  )
}

export function introChips(): string[] {
  return ["What can you do?"]
}

export function actionChips(isEmployee: boolean): string[] {
  return isEmployee ? OPS_ACTIONS : USER_ACTIONS
}

export function capabilityReply(isEmployee: boolean): string {
  return isEmployee
    ? "I'm the reconciliation console. I work on the live ledger and propose corrective entries - nothing executes without your approval. Here's everything I can do:\n\n" +
      "\u2022 Reconcile an account - \"Reconcile checking\" - verify the balance against the ledger\n" +
      "\u2022 Diagnose a break - root-cause evidence from the entries\n" +
      "\u2022 Propose corrective entries - staged, approval-gated\n\n" +
      "Pick a prompt below, or type your own."
    : "I'm your private banking agent. I work on the real ledger - and I never move money without you. Here's everything I can do:\n\n" +
      "\u2022 Check balances - \"What's my balance?\"\n" +
      "\u2022 Show recent transactions - \"Show my transactions\"\n" +
      "\u2022 Move money between accounts - \"Transfer $50 to savings\" - prepared first, executed only after you approve\n" +
      "\u2022 Analyze your spending - \"Analyze my spending\" - categorized from your real ledger, so you can see where your money goes\n\n" +
      "That's the whole menu. Pick a prompt below, or type your own."
}

export function followUpChips(isEmployee: boolean, lastUserText: string): string[] {
  const t = lastUserText.toLowerCase()
  if (isAnalyzeQuestion(t)) {
    return isEmployee ? OPS_ACTIONS : USER_ACTIONS.filter((c) => !c.toLowerCase().includes("analyze"))
  }
  if (t.includes("transaction") || t.includes("activit") || t.includes("ledger") || t.includes("movement")) {
    return isEmployee
      ? OPS_ACTIONS
      : USER_ACTIONS.filter((c) => !c.toLowerCase().includes("transaction"))
  }
  if (t.includes("transfer") || t.includes("move") || t.startsWith("approved")) {
    return isEmployee
      ? OPS_ACTIONS
      : USER_ACTIONS.filter((c) => !c.toLowerCase().includes("transfer"))
  }
  return actionChips(isEmployee)
}