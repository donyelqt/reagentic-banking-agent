package dev.reagentic.ai.agent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentServiceTest {

    private final Planner planner = mock(Planner.class);
    private final Executor executor = mock(Executor.class);
    private final AgentService service = new AgentService(planner, executor);

    private static Step reconcileStep() {
        return new Step("reconcile-1", "backend", "reconcileAccount",
                Map.of("accountId", "acc-checking-0001"), List.of(), false, null);
    }

    private static Map<String, Object> evidenceEntry(long id, String type, String signed,
                                                     String paymentId, String balanceAfter) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("entryId", id);
        e.put("type", type);
        e.put("signedAmount", signed);
        e.put("balanceAfter", balanceAfter);
        e.put("paymentId", paymentId);
        return e;
    }

    private static Map<String, Object> mismatchResult() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("accountId", "acc-checking-0001");
        m.put("balance", "950.00");
        m.put("ledgerSum", "1000.00");
        m.put("delta", "-50.00");
        m.put("balanced", false);
        m.put("direction", "MISSING_DEBIT_LEG");
        m.put("missingAmount", "50.00");
        m.put("lastEntryId", 2);
        m.put("lastPaymentId", "pmt-2");
        m.put("lastBalanceAfter", "1000.00");
        m.put("diagnosis", "Account balance (950.00) does not equal the immutable ledger.");
        List<Map<String, Object>> evidence = new ArrayList<>();
        evidence.add(evidenceEntry(1, "OPENING", "1000.00", "OPENING", "1000.00"));
        evidence.add(evidenceEntry(2, "DEBIT", "-50.00", "pmt-2", "950.00"));
        m.put("evidenceCount", 13);
        m.put("evidence", evidence);
        return m;
    }

    @Test
    void mismatchReplySurfacesEvidenceTrailAndProposedCorrectiveEntry() {
        Plan plan = new Plan(List.of(reconcileStep()));
        Executor.ExecResult exec = new Executor.ExecResult(
                List.of(new StepResult("reconcile-1", true, mismatchResult(), null)), List.of());
        when(executor.execute(eq(plan), anyList(), eq("tok"), eq("EMPLOYEE"))).thenReturn(exec);

        AgentResponse resp = service.chat(new ChatRequest("reconcile acc-checking-0001", null, plan.steps(), null, null), "tok", "EMPLOYEE");

        String reply = resp.reply();
        assertTrue(reply.contains("Reconciliation MISMATCH on acc-checking-0001"), reply);
        assertTrue(reply.contains("MISSING_DEBIT_LEG of 50.00"), reply);
        assertTrue(reply.contains("Evidence trail (last 12 of 13 ledger entries)"), reply);
        assertTrue(reply.contains("DEBIT -50.00"), reply);
        assertTrue(reply.contains("Proposed corrective journal entry (NOT executed - requires ops review)"), reply);
        assertTrue(reply.contains("Dr. Account acc-checking-0001  50.00"), reply);
        assertTrue(reply.contains("Cr. Ledger-Suspense-acc-checking-0001  50.00"), reply);
        assertFalse(reply.contains("Awaiting your approval"), reply);
        verify(executor).execute(eq(plan), anyList(), eq("tok"), eq("EMPLOYEE"));
    }

    @Test
    void balancedReplyReportsCleanReconciliation() {
        Map<String, Object> balanced = new LinkedHashMap<>();
        balanced.put("accountId", "acc-checking-0001");
        balanced.put("balance", "1000.00");
        balanced.put("ledgerSum", "1000.00");
        balanced.put("balanced", true);
        Plan plan = new Plan(List.of(reconcileStep()));
        Executor.ExecResult exec = new Executor.ExecResult(
                List.of(new StepResult("reconcile-1", true, balanced, null)), List.of());
        when(executor.execute(any(), anyList(), eq("tok"), any())).thenReturn(exec);

        AgentResponse resp = service.chat(new ChatRequest("reconcile acc-checking-0001", null, plan.steps(), null, null), "tok", "EMPLOYEE");

        assertTrue(resp.reply().contains("is BALANCED"), resp.reply());
        assertFalse(resp.reply().contains("Evidence trail"), resp.reply());
    }

    @Test
    void failedStepReportsErrorWithoutSplitting() {
        Plan plan = new Plan(List.of(reconcileStep()));
        Executor.ExecResult exec = new Executor.ExecResult(
                List.of(new StepResult("reconcile-1", false, null, "backend call failed")), List.of());
        when(executor.execute(any(), anyList(), eq("tok"), any())).thenReturn(exec);

        AgentResponse resp = service.chat(new ChatRequest("reconcile acc-checking-0001", null, plan.steps(), null, null), "tok", "EMPLOYEE");

        assertTrue(resp.reply().contains("Step reconcile-1 failed: backend call failed"), resp.reply());
    }

    @Test
    void customerIsDeniedReconcileTool() {
        Step reconcile = reconcileStep();
        Plan plan = new Plan(List.of(reconcile));
        when(executor.execute(eq(plan), anyList(), eq("tok"), eq("USER"))).thenReturn(new Executor.ExecResult(
                List.of(new StepResult("reconcile-1", false, null,
                        "reconcileAccount requires the EMPLOYEE (ops analyst) role")), List.of()));

        AgentResponse resp = service.chat(new ChatRequest("reconcile checking", null, plan.steps(), null, null), "tok", "USER");

        assertTrue(resp.reply().contains("requires the EMPLOYEE (ops analyst) role"), resp.reply());
    }

    @Test
    void balanceQueryRepliesWithAccountAndAmount() {
        Step balance = new Step("s1", "backend", "getBalance",
                Map.of("accountId", "acc-savings-0002"), List.of(), false, null);
        Plan plan = new Plan(List.of(balance));
        Executor.ExecResult exec = new Executor.ExecResult(
                List.of(new StepResult("s1", true, "800.00", null)), List.of());
        when(executor.execute(eq(plan), anyList(), eq("tok"), eq("USER"))).thenReturn(exec);

        AgentResponse resp = service.chat(new ChatRequest("what is my balance", null, plan.steps(), null, null), "tok", "USER");

        assertTrue(resp.reply().contains("Balance of account acc-savings-0002: $800.00"), resp.reply());
    }

    @Test
    void transactionsQuerySummarizesLatestEntries() {
        Step txns = new Step("s1", "backend", "listTransactions",
                Map.of("accountId", "acc-checking-0001"), List.of(), false, null);
        Plan plan = new Plan(List.of(txns));
        List<Map<String, Object>> entries = List.of(
                Map.of("entryId", 1, "type", "OPENING", "signedAmount", "1000.00", "balanceAfter", "1000.00"),
                Map.of("entryId", 2, "type", "DEBIT", "signedAmount", "-50.00", "balanceAfter", "950.00"));
        Executor.ExecResult exec = new Executor.ExecResult(
                List.of(new StepResult("s1", true, entries, null)), List.of());
        when(executor.execute(eq(plan), anyList(), eq("tok"), eq("USER"))).thenReturn(exec);

        AgentResponse resp = service.chat(new ChatRequest("show my transactions", null, plan.steps(), null, null), "tok", "USER");

        assertTrue(resp.reply().contains("Ledger for account: 2 entries"), resp.reply());
        assertTrue(resp.reply().contains("#2 DEBIT -50.00"), resp.reply());
    }

    @Test
    void transferApprovalReplyReportsPayment() {
        Step transfer = new Step("transfer-1", "backend", "transferFunds",
                Map.of("from", "acc-checking-0001", "to", "acc-savings-0002", "amount", "25.00"),
                List.of(), true, "k-1");
        Plan plan = new Plan(List.of(transfer));
        Map<String, Object> payment = new LinkedHashMap<>();
        payment.put("paymentId", "pmt-1");
        payment.put("status", "COMPLETED");
        Executor.ExecResult exec = new Executor.ExecResult(
                List.of(new StepResult("transfer-1", true, payment, null)), List.of());
        when(executor.execute(eq(plan), eq(List.of("transfer-1")), eq("tok"), eq("USER"))).thenReturn(exec);

        AgentResponse resp = service.chat(new ChatRequest("approve", null, plan.steps(), List.of("transfer-1"), null), "tok", "USER");

        assertTrue(resp.reply().contains("Transfer executed: payment pmt-1 status=COMPLETED"), resp.reply());
    }

    @Test
    void employeeIsDeniedTransferTool() {
        Step transfer = new Step("transfer-1", "backend", "transferFunds",
                Map.of("from", "acc-checking-0001", "to", "acc-savings-0002", "amount", "50.00"),
                List.of(), true, "k-1");
        Plan plan = new Plan(List.of(transfer));
        when(executor.execute(eq(plan), anyList(), eq("tok"), eq("EMPLOYEE"))).thenReturn(new Executor.ExecResult(
                List.of(new StepResult("transfer-1", false, null,
                        "EMPLOYEE role cannot execute transfers - ops powers are investigation-only")), List.of()));

        AgentResponse resp = service.chat(new ChatRequest("transfer 50", null, plan.steps(), null, null), "tok", "EMPLOYEE");

        assertTrue(resp.reply().contains("ops powers are investigation-only"), resp.reply());
    }
}