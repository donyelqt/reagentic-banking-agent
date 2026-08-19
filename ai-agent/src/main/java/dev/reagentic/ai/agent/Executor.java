package dev.reagentic.ai.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class Executor {

    private static final Logger log = LoggerFactory.getLogger(Executor.class);

    private final AgentWorkers workers;

    public Executor(AgentWorkers workers) {
        this.workers = workers;
    }

    public record ExecResult(List<StepResult> results, List<Step> pending) {
    }

    public ExecResult execute(Plan plan, List<String> approved, String token, String role) {
        List<Step> ordered = topoSort(plan.steps());
        Set<String> approvedSet = new HashSet<>(approved == null ? List.of() : approved);
        List<StepResult> results = new ArrayList<>();
        List<Step> pending = new ArrayList<>();
        for (Step step : ordered) {
            String gate = roleGate(step.tool(), role);
            if (gate != null) {
                results.add(new StepResult(step.stepId(), false, null, gate));
                continue;
            }
            // Money-movement steps always require human approval, regardless of what a
            // planner or client-supplied plan claims. Trust-boundary guard (Story 1,
            // criterion 4): a transferFunds step can never execute without an explicit
            // approval, even if confirmationRequired arrived as false.
            boolean needsApproval = step.confirmationRequired()
                    || "transferFunds".equals(step.tool());
            if (needsApproval && !approvedSet.contains(step.stepId())) {
                pending.add(step);
                continue;
            }
            results.add(executeStep(step, token, role));
        }
        return new ExecResult(results, pending);
    }

    private String roleGate(String tool, String role) {
        boolean employee = "EMPLOYEE".equals(role);
        if ("reconcileAccount".equals(tool) && !employee) {
            return "reconcileAccount requires the EMPLOYEE (ops analyst) role";
        }
        if ("transferFunds".equals(tool) && employee) {
            return "EMPLOYEE role cannot execute transfers - ops powers are investigation-only";
        }
        return null;
    }

    public Map<String, Object> reconcileDirect(String accountId, String token, String role) {
        if (!"EMPLOYEE".equals(role)) {
            throw new RuntimeException("reconcileAccount requires the EMPLOYEE (ops analyst) role");
        }
        return workers.reconcile(token, accountId);
    }

    private StepResult executeStep(Step step, String token, String role) {
        boolean employee = "EMPLOYEE".equals(role);
        try {
            Object data = switch (step.tool()) {
                case "listAccounts" -> workers.listAccounts(token);
                case "getBalance" -> workers.getBalance(token, str(step.args().get("accountId")), employee);
                case "listTransactions" -> workers.listTransactions(token, str(step.args().get("accountId")), employee);
                case "transferFunds" -> {
                    if (employee) {
                        throw new RuntimeException(
                                "EMPLOYEE role cannot execute transfers - ops powers are investigation-only");
                    }
                    yield workers.transfer(token,
                            str(step.args().get("from")), str(step.args().get("to")),
                            str(step.args().get("amount")), idempotencyKey(step));
                }
                case "reconcileAccount" -> {
                    if (!employee) {
                        throw new RuntimeException("reconcileAccount requires the EMPLOYEE (ops analyst) role");
                    }
                    yield workers.reconcile(token, str(step.args().get("accountId")));
                }
                default -> throw new RuntimeException("unknown tool: " + step.tool());
            };
            return new StepResult(step.stepId(), true, data, null);
        } catch (Exception e) {
            log.warn("executor step {} ({}) failed: {}", step.stepId(), step.tool(), e.getMessage());
            return new StepResult(step.stepId(), false, null, e.getMessage());
        }
    }

    private String str(Object o) {
        return o == null ? null : o.toString();
    }

    private String idempotencyKey(Step step) {
        String fromArgs = str(step.args().get("idempotencyKey"));
        if (fromArgs != null && !fromArgs.isBlank()) {
            return fromArgs;
        }
        return step.idempotencyKey();
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
