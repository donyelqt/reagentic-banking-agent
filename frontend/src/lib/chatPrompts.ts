const USER_ACTIONS = ["What's my balance?", "Show my transactions", "Transfer $50 to savings"]
const OPS_ACTIONS = ["Reconcile checking", "Reconcile savings"]

export function isCapabilityQuestion(text: string): boolean {
  const t = text.trim().toLowerCase()
  return /what can (you|i) do|what do you do|show me what you can do|capabilit|commands|can you help|\bhelp\b/.test(t)
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
      "\u2022 Move money between accounts - \"Transfer $50 to savings\" - prepared first, executed only after you approve\n\n" +
      "That's the whole menu. Pick a prompt below, or type your own."
}

export function followUpChips(isEmployee: boolean, lastUserText: string): string[] {
  const t = lastUserText.toLowerCase()
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