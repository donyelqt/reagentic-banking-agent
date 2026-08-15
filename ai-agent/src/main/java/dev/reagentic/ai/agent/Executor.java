package dev.reagentic.ai.agent;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class Executor {

    private final AgentWorkers workers;

    public Executor(AgentWorkers workers) {
        this.workers = workers;
    }

    public record ExecResult(List<StepResult> results, List<Step> pending) {
    }

    public ExecResult execute(Plan plan, List<String> approved, String token) {
        List<Step> ordered = topoSort(plan.steps());
        Set<String> approvedSet = new HashSet<>(approved == null ? List.of() : approved);
        List<StepResult> results = new ArrayList<>();
        List<Step> pending = new ArrayList<>();
        for (Step step : ordered) {
            if (step.confirmationRequired() && !approvedSet.contains(step.stepId())) {
                pending.add(step);
                continue;
            }
            results.add(executeStep(step, token));
        }
        return new ExecResult(results, pending);
    }

    private StepResult executeStep(Step step, String token) {
        try {
            Object data = switch (step.tool()) {
                case "listAccounts" -> workers.listAccounts(token);
                case "getBalance" -> workers.getBalance(token, str(step.args().get("accountId")));
                case "listTransactions" -> workers.listTransactions(token, str(step.args().get("accountId")));
                case "transferFunds" -> workers.transfer(token,
                        str(step.args().get("from")), str(step.args().get("to")),
                        str(step.args().get("amount")), str(step.args().get("idempotencyKey")));
                case "reconcileAccount" -> workers.reconcile(token, str(step.args().get("accountId")));
                default -> throw new RuntimeException("unknown tool: " + step.tool());
            };
            return new StepResult(step.stepId(), true, data, null);
        } catch (Exception e) {
            return new StepResult(step.stepId(), false, null, e.getMessage());
        }
    }

    private String str(Object o) {
        return o == null ? null : o.toString();
    }

    private List<Step> topoSort(List<Step> steps) {
        Map<String, Step> byId = new HashMap<>();
        for (Step s : steps) {
            byId.put(s.stepId(), s);
        }
        Set<String> visited = new HashSet<>();
        Set<String> inProgress = new LinkedHashSet<>();
        List<Step> ordered = new ArrayList<>();
        for (Step s : steps) {
            visit(s, byId, visited, inProgress, ordered);
        }
        return ordered;
    }

    private void visit(Step s, Map<String, Step> byId, Set<String> visited,
                       Set<String> inProgress, List<Step> ordered) {
        if (visited.contains(s.stepId())) {
            return;
        }
        inProgress.add(s.stepId());
        for (String dep : (s.dependsOn() == null ? List.<String>of() : s.dependsOn())) {
            Step d = byId.get(dep);
            if (d != null && !visited.contains(dep)) {
                if (inProgress.contains(dep)) {
                    continue; // cycle guard
                }
                visit(d, byId, visited, inProgress, ordered);
            }
        }
        inProgress.remove(s.stepId());
        visited.add(s.stepId());
        ordered.add(s);
    }
}
