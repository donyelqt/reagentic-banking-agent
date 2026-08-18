package dev.reagentic.ai.agent;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AgentService {

    private final Planner planner;
    private final Executor executor;

    public AgentService(Planner planner, Executor executor) {
        this.planner = planner;
        this.executor = executor;
    }

    public AgentResponse chat(ChatRequest req, String token, String role) {
        Plan plan;
        List<String> approved;
        if (req.plan() != null && !req.plan().isEmpty()) {
            plan = new Plan(req.plan());
            approved = req.approval() == null ? List.of() : req.approval();
        } else {
            plan = planner.plan(req.message(), req.history());
            approved = List.of();
        }
        Executor.ExecResult exec = executor.execute(plan, approved, token, role);
        String reply = buildReply(plan, exec, req.message());
        return new AgentResponse(plan.steps(), exec.results(), exec.pending(), reply);
    }

    private String buildReply(Plan plan, Executor.ExecResult exec, String message) {
        if (plan.steps().isEmpty()) {
            return "I can help with: list accounts, balances, transactions, transfers (with your approval), "
                    + "and reconciling an account. Try \"transfer 50 to savings\" or \"reconcile my checking account\".";
        }
        java.util.Map<String, String> toolByStep = new java.util.HashMap<>();
        java.util.Map<String, java.util.Map<String, Object>> argsByStep = new java.util.HashMap<>();
        for (Step s : plan.steps()) {
            toolByStep.put(s.stepId(), s.tool());
            argsByStep.put(s.stepId(), s.args());
        }
        StringBuilder sb = new StringBuilder();
        for (StepResult r : exec.results()) {
            if (!r.ok()) {
                sb.append("Step ").append(r.stepId()).append(" failed: ").append(r.error()).append("\n");
                continue;
            }
            if (r.data() instanceof com.fasterxml.jackson.databind.node.ObjectNode on) {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                on.properties().forEach(e -> {
                    com.fasterxml.jackson.databind.JsonNode v = e.getValue();
                    m.put(e.getKey(), v.isValueNode() ? v.asText() : v);
                });
                r = new StepResult(r.stepId(), r.ok(), m, r.error());
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
                String formatted = formatResult(r, toolByStep.get(r.stepId()), argsByStep.get(r.stepId()));
                if (formatted != null) {
                    sb.append(formatted).append("\n");
                }
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

    private String formatResult(StepResult r, String tool, java.util.Map<String, Object> args) {
        Object data = r.data();
        if (data == null) {
            return null;
        }
        if (data instanceof com.fasterxml.jackson.databind.node.ArrayNode an) {
            java.util.List<Map<String, Object>> items = new java.util.ArrayList<>();
            for (com.fasterxml.jackson.databind.JsonNode n : an) {
                if (n instanceof com.fasterxml.jackson.databind.node.ObjectNode o) {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    o.properties().forEach(e -> {
                        com.fasterxml.jackson.databind.JsonNode v = e.getValue();
                        m.put(e.getKey(), v.isValueNode() ? v.asText() : v);
                    });
                    items.add(m);
                }
            }
            data = items;
        }
        if ("getBalance".equals(tool)) {
            return "Balance of account " + (args == null ? "?" : args.get("accountId")) + ": $" + data + ".";
        }
        if ("listAccounts".equals(tool) && data instanceof java.util.List<?> accounts) {
            StringBuilder sb = new StringBuilder("You have ").append(accounts.size()).append(" account");
            if (accounts.size() != 1) {
                sb.append("s");
            }
            sb.append(":");
            for (Object a : accounts) {
                if (a instanceof java.util.Map<?, ?> m) {
                    sb.append(" ").append(String.valueOf(m.get("type")).toLowerCase())
                            .append(" (").append(m.get("accountId")).append(") $").append(m.get("balance")).append(",");
                }
            }
            return sb.substring(0, sb.length() - 1) + ".";
        }
        if ("listTransactions".equals(tool) && data instanceof java.util.List<?> entries) {
            StringBuilder sb = new StringBuilder("Ledger for account: ").append(entries.size()).append(" entries. Latest:");
            int from = Math.max(0, entries.size() - 5);
            for (int i = from; i < entries.size(); i++) {
                Object e = entries.get(i);
                if (e instanceof java.util.Map<?, ?> m) {
                    sb.append(" #").append(m.get("entryId")).append(" ").append(m.get("type"))
                            .append(" ").append(m.get("signedAmount"))
                            .append(" (balanceAfter ").append(m.get("balanceAfter")).append("),");
                }
            }
            return sb.substring(0, sb.length() - 1) + ".";
        }
        return null;
    }
}