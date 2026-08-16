package dev.reagentic.ai.agent;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentService {

    private final Planner planner;
    private final AgentWorkers workers;
    private final Executor executor;

    public AgentService(Planner planner, AgentWorkers workers, Executor executor) {
        this.planner = planner;
        this.workers = workers;
        this.executor = executor;
    }

    public AgentResponse chat(ChatRequest req, String token) {
        Plan plan;
        List<String> approved;
        if (req.plan() != null && !req.plan().isEmpty()) {
            plan = new Plan(req.plan());
            approved = req.approval() == null ? List.of() : req.approval();
        } else {
            plan = planner.plan(req.message());
            approved = List.of();
        }
        Executor.ExecResult exec = executor.execute(plan, approved, token);
        String reply = buildReply(plan, exec, req.message());
        return new AgentResponse(plan.steps(), exec.results(), exec.pending(), reply);
    }

    private String buildReply(Plan plan, Executor.ExecResult exec, String message) {
        if (plan.steps().isEmpty()) {
            return "I can help with: list accounts, balances, transactions, transfers (with your approval), "
                    + "and reconciling an account. Try \"transfer 50 to savings\" or \"reconcile my checking account\".";
        }
        StringBuilder sb = new StringBuilder();
        for (StepResult r : exec.results()) {
            if (!r.ok()) {
                sb.append("Step ").append(r.stepId()).append(" failed: ").append(r.error()).append("\n");
                continue;
            }
            if (r.data() instanceof java.util.Map<?, ?> m && m.containsKey("balanced")) {
                boolean balanced = Boolean.parseBoolean(String.valueOf(m.get("balanced")));
                if (balanced) {
                    sb.append("Reconciliation: account ").append(m.get("accountId"))
                            .append(" is BALANCED (balance=").append(m.get("balance"))
                            .append(", ledgerSum=").append(m.get("ledgerSum")).append(").\n");
                } else {
                    sb.append("Reconciliation MISMATCH on ").append(m.get("accountId"))
                            .append(": balance=").append(m.get("balance"))
                            .append(", ledgerSum=").append(m.get("ledgerSum"))
                            .append(", delta=").append(m.get("delta")).append(".\n");
                    sb.append("Root cause: ").append(m.get("direction"))
                            .append(" of ").append(m.get("missingAmount"))
                            .append(" (account reports ").append(m.get("balance"))
                            .append(" but ledger ends at balanceAfter=").append(m.get("lastBalanceAfter"))
                            .append(", entry #").append(m.get("lastEntryId")).append(").\n");
                    sb.append(m.get("diagnosis")).append("\n");
                    sb.append("\nEvidence trail (last 12 of ").append(m.get("evidenceCount")).append(" ledger entries):\n");
                    if (m.get("evidence") instanceof java.util.List<?> ev) {
                        for (Object e : ev) {
                            if (e instanceof java.util.Map<?, ?> em) {
                                sb.append("  #").append(em.get("entryId"))
                                        .append(" ").append(em.get("type"))
                                        .append(" ").append(em.get("signedAmount"))
                                        .append(" (payment ").append(em.get("paymentId"))
                                        .append(") balanceAfter=").append(em.get("balanceAfter")).append("\n");
                            }
                        }
                    }
                    sb.append("\nProposed corrective journal entry (NOT executed - requires ops review):\n");
                    boolean debitLeg = "MISSING_DEBIT_LEG".equals(m.get("direction"));
                    sb.append("  ").append(debitLeg ? "Dr." : "Cr.").append(" Account ").append(m.get("accountId"))
                            .append("  ").append(m.get("missingAmount")).append("\n");
                    sb.append("  ").append(debitLeg ? "Cr." : "Dr.").append(" Ledger-Suspense-").append(m.get("accountId"))
                            .append("  ").append(m.get("missingAmount")).append("\n");
                }
            } else if (r.data() instanceof java.util.Map<?, ?> pm && pm.containsKey("paymentId")) {
                sb.append("Transfer executed: payment ").append(pm.get("paymentId"))
                        .append(" status=").append(pm.get("status")).append(".\n");
            } else {
                sb.append("Step ").append(r.stepId()).append(" completed.\n");
            }
        }
        if (!exec.pending().isEmpty()) {
            sb.append("Awaiting your approval for: ");
            for (Step p : exec.pending()) {
                sb.append(p.tool()).append("(").append(p.args()).append(") ");
            }
            sb.append("- approve to execute.");
        }
        return sb.toString().trim();
    }
}