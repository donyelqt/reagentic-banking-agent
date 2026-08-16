package dev.reagentic.ai.agent;

import dev.reagentic.common.DemoConstants;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic, LLM-free planner. Mandatory safety net: the agent always
 * produces a valid plan DAG even when no LLM is available, so the demo runs
 * with zero external dependencies. An LLM planner can later replace this as the
 * primary path (the plan's documented upgrade point) without changing workers
 * or the executor.
 */
@Component
public class KeywordPlanner implements Planner {

    private static final Pattern AMOUNT = Pattern.compile("(\\d+(?:\\.\\d+)?)");
    private static final Pattern ACC_ID = Pattern.compile("(acc-[a-z0-9-]+)");

    @Override
    public Plan plan(String message) {
        String m = message == null ? "" : message.toLowerCase();
        if (m.isBlank()) {
            return new Plan(List.of());
        }
        if (m.contains("transfer") || m.contains("send") || m.contains("move")) {
            Step s = transferStep(m);
            if (s != null) {
                return new Plan(List.of(s));
            }
        }
        if (m.contains("reconcile") || m.contains("balance") && m.contains("why")) {
            Step s = reconcileStep(m);
            if (s != null) {
                return new Plan(List.of(s));
            }
        }
        if (m.contains("reconcile")) {
            Step s = reconcileStep(m);
            if (s != null) {
                return new Plan(List.of(s));
            }
        }
        if (m.contains("transaction") || m.contains("statement") || m.contains("ledger")) {
            Step s = accountStep(m, "listTransactions");
            if (s != null) {
                return new Plan(List.of(s));
            }
        }
        if (m.contains("balance")) {
            Step s = accountStep(m, "getBalance");
            if (s != null) {
                return new Plan(List.of(s));
            }
        }
        if (m.contains("account") || m.contains("list")) {
            return new Plan(List.of(new Step("s1", "backend", "listAccounts",
                    Map.of(), List.of(), false, null)));
        }
        return new Plan(List.of());
    }

    private Step transferStep(String m) {
        Matcher am = AMOUNT.matcher(m);
        if (!am.find()) {
            return null;
        }
        BigDecimal amount = new BigDecimal(am.group(1)).setScale(2, java.math.RoundingMode.HALF_UP);
        AccountSide side = detectSides(m);
        if (side.from() == null || side.to() == null) {
            return null;
        }
        Map<String, Object> args = new HashMap<>();
        args.put("from", side.from());
        args.put("to", side.to());
        args.put("amount", amount.toPlainString());
        args.put("idempotencyKey", UUID.randomUUID().toString());
        return new Step("transfer-1", "backend", "transferFunds", args, List.of(), true,
                (String) args.get("idempotencyKey"));
    }

    private Step reconcileStep(String m) {
        String acc = resolveAccount(m);
        if (acc == null) {
            return null;
        }
        Map<String, Object> args = new HashMap<>();
        args.put("accountId", acc);
        return new Step("reconcile-1", "backend", "reconcileAccount", args, List.of(), false, null);
    }

    private Step accountStep(String m, String tool) {
        String acc = resolveAccount(m);
        if (acc == null) {
            return null;
        }
        Map<String, Object> args = new HashMap<>();
        args.put("accountId", acc);
        return new Step("s1", "backend", tool, args, List.of(), false, null);
    }

    private String resolveAccount(String m) {
        Matcher id = ACC_ID.matcher(m);
        if (id.find()) {
            return id.group(1);
        }
        if (m.contains("savings")) {
            return DemoConstants.SAVINGS_ACCOUNT_ID;
        }
        if (m.contains("checking")) {
            return DemoConstants.CHECKING_ACCOUNT_ID;
        }
        // default to checking for ambiguous reads
        return DemoConstants.CHECKING_ACCOUNT_ID;
    }

    private AccountSide detectSides(String m) {
        boolean savings = m.contains("savings");
        boolean checking = m.contains("checking");
        if (savings && checking) {
            String fromType = null;
            String toType = null;
            Matcher fromM = Pattern.compile("from\\s+(savings|checking)").matcher(m);
            if (fromM.find()) {
                fromType = fromM.group(1);
            }
            Matcher toM = Pattern.compile("to\\s+(savings|checking)").matcher(m);
            if (toM.find()) {
                toType = toM.group(1);
            }
            if (fromType != null && toType != null) {
                return new AccountSide(idFor(fromType), idFor(toType));
            }
            int ci = m.indexOf("checking");
            int si = m.indexOf("savings");
            String first = ci < si ? "checking" : "savings";
            String second = ci < si ? "savings" : "checking";
            return new AccountSide(idFor(first), idFor(second));
        } else if (savings) {
            return new AccountSide(DemoConstants.CHECKING_ACCOUNT_ID, DemoConstants.SAVINGS_ACCOUNT_ID);
        } else if (checking) {
            return new AccountSide(DemoConstants.SAVINGS_ACCOUNT_ID, DemoConstants.CHECKING_ACCOUNT_ID);
        }
        return new AccountSide(null, null);
    }

    private String idFor(String type) {
        return "savings".equals(type) ? DemoConstants.SAVINGS_ACCOUNT_ID : DemoConstants.CHECKING_ACCOUNT_ID;
    }

    private record AccountSide(String from, String to) {
    }
}
