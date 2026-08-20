package dev.reagentic.ai.agent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private final AgentService service = new AgentService(planner, executor, new PendingApprovalStore());

    private static Step reconcileStep() {
        return new Step("reconcile-1", "backend", "reconcileAccount",
                Map.of("accountId", "acc-checking-0001"), List.of(), false, null);
    }

    private static Step transferStep() {
        return new Step("transfer-1", "backend", "transferFunds",
                Map.of("from", "acc-checking-0001", "to", "acc-savings-0002", "amount", "25.00"),
                List.of(), true, "k-1");
    }

    private static ChatRequest msg(String message) {
        return new ChatRequest(message, null, null, null, null, null);
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
        when(planner.plan(eq("reconcile acc-checking-0001"), any())).thenReturn(plan);
        Executor.ExecResult exec = new Executor.ExecResult(
                List.of(new StepResult("reconcile-1", true, mismatchResult(), null)), List.of());
        when(executor.execute(eq(plan), anyList(), eq("tok"), eq("EMPLOYEE"))).thenReturn(exec);

        AgentResponse resp = service.chat(msg("reconcile acc-checking-0001"), "tok", "EMPLOYEE", "ops@bank.dev");

        String reply = resp.reply();
        assertTrue(reply.contains("Reconciliation MISMATCH on acc-checking-0001"), reply);
        assertTrue(reply.contains("MISSING_DEBIT_LEG of 50.00"), reply);
        assertTrue(reply.contains("Evidence trail (last 12 of 13 ledger entries)"), reply);
        assertTrue(reply.contains("DEBIT -50.00"), reply);
        assertTrue(reply.contains("Proposed corrective journal entry (NOT executed - requires ops review)"), reply);
        assertTrue(reply.contains("Dr. Account acc-checking-0001  50.00"), reply);
        assertTrue(reply.contains("Cr. Ledger-Suspense-acc-checking-0001  50.00"), reply);
        assertFalse(reply.contains("Awaiting your approval"), reply);
        assertNull(resp.approvalId());
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
        when(planner.plan(any(), any())).thenReturn(plan);
        Executor.ExecResult exec = new Executor.ExecResult(
                List.of(new StepResult("reconcile-1", true, balanced, null)), List.of());
        when(executor.execute(any(), anyList(), eq("tok"), any())).thenReturn(exec);

        AgentResponse resp = service.chat(msg("reconcile acc-checking-0001"), "tok", "EMPLOYEE", "ops@bank.dev");

        assertTrue(resp.reply().contains("is BALANCED"), resp.reply());
        assertFalse(resp.reply().contains("Evidence trail"), resp.reply());
    }

    @Test
    void failedStepReportsErrorWithoutSplitting() {
        Plan plan = new Plan(List.of(reconcileStep()));
        when(planner.plan(any(), any())).thenReturn(plan);
        Executor.ExecResult exec = new Executor.ExecResult(
                List.of(new StepResult("reconcile-1", false, null, "backend call failed")), List.of());
        when(executor.execute(any(), anyList(), eq("tok"), any())).thenReturn(exec);

        AgentResponse resp = service.chat(msg("reconcile acc-checking-0001"), "tok", "EMPLOYEE", "ops@bank.dev");

        assertTrue(resp.reply().contains("Step reconcile-1 failed: backend call failed"), resp.reply());
    }

    @Test
    void customerIsDeniedReconcileTool() {
        Step reconcile = reconcileStep();
        Plan plan = new Plan(List.of(reconcile));
        when(planner.plan(any(), any())).thenReturn(plan);
        when(executor.execute(eq(plan), anyList(), eq("tok"), eq("USER"))).thenReturn(new Executor.ExecResult(
                List.of(new StepResult("reconcile-1", false, null,
                        "reconcileAccount requires the EMPLOYEE (ops analyst) role")), List.of()));

        AgentResponse resp = service.chat(msg("reconcile checking"), "tok", "USER", "demo@bank.dev");

        assertTrue(resp.reply().contains("requires the EMPLOYEE (ops analyst) role"), resp.reply());
    }

    @Test
    void balanceQueryRepliesWithAccountAndAmount() {
        Step balance = new Step("s1", "backend", "getBalance",
                Map.of("accountId", "acc-savings-0002"), List.of(), false, null);
        Plan plan = new Plan(List.of(balance));
        when(planner.plan(any(), any())).thenReturn(plan);
        Executor.ExecResult exec = new Executor.ExecResult(
                List.of(new StepResult("s1", true, "800.00", null)), List.of());
        when(executor.execute(eq(plan), anyList(), eq("tok"), eq("USER"))).thenReturn(exec);

        AgentResponse resp = service.chat(msg("what is my balance"), "tok", "USER", "demo@bank.dev");

        assertTrue(resp.reply().contains("Balance of account acc-savings-0002: $800.00"), resp.reply());
    }

    @Test
    void transactionsQuerySummarizesLatestEntries() {
        Step txns = new Step("s1", "backend", "listTransactions",
                Map.of("accountId", "acc-checking-0001"), List.of(), false, null);
        Plan plan = new Plan(List.of(txns));
        when(planner.plan(any(), any())).thenReturn(plan);
        List<Map<String, Object>> entries = List.of(
                Map.of("entryId", 1, "type", "OPENING", "signedAmount", "1000.00", "balanceAfter", "1000.00"),
                Map.of("entryId", 2, "type", "DEBIT", "signedAmount", "-50.00", "balanceAfter", "950.00"));
        Executor.ExecResult exec = new Executor.ExecResult(
                List.of(new StepResult("s1", true, entries, null)), List.of());
        when(executor.execute(eq(plan), anyList(), eq("tok"), eq("USER"))).thenReturn(exec);

        AgentResponse resp = service.chat(msg("show my transactions"), "tok", "USER", "demo@bank.dev");

        assertTrue(resp.reply().contains("Ledger for account: 2 entries"), resp.reply());
        assertTrue(resp.reply().contains("#2 DEBIT -50.00"), resp.reply());
    }

    @Test
    void transferIssuesServerHeldApprovalIdThenExecutesOnApprovalEcho() {
        Plan plan = new Plan(List.of(transferStep()));
        when(planner.plan(eq("transfer 25"), any())).thenReturn(plan);
        when(executor.execute(eq(plan), eq(List.of()), eq("tok"), eq("USER"))).thenReturn(
                new Executor.ExecResult(List.of(), List.of(transferStep())));
        Map<String, Object> payment = new LinkedHashMap<>();
        payment.put("paymentId", "pmt-1");
        payment.put("status", "COMPLETED");
        when(executor.execute(eq(plan), eq(List.of("transfer-1")), eq("tok"), eq("USER"))).thenReturn(
                new Executor.ExecResult(List.of(new StepResult("transfer-1", true, payment, null)), List.of()));

        // Phase 1: message -> server plans and persists an approval session.
        AgentResponse first = service.chat(msg("transfer 25"), "tok", "USER", "demo@bank.dev");
        assertNotNull(first.approvalId());
        assertEquals(1, first.pendingSteps().size());
        assertTrue(first.reply().contains("Awaiting your approval"), first.reply());

        // Phase 2: approval echo references the server-issued session only.
        AgentResponse second = service.chat(
                new ChatRequest(null, null, null, List.of("transfer-1"), null, first.approvalId()),
                "tok", "USER", "demo@bank.dev");
        assertTrue(second.reply().contains("Transfer executed: payment pmt-1 status=COMPLETED"), second.reply());

        verify(executor).execute(eq(plan), eq(List.of()), eq("tok"), eq("USER"));
        verify(executor).execute(eq(plan), eq(List.of("transfer-1")), eq("tok"), eq("USER"));
    }

    @Test
    void clientSuppliedPlanWithoutApprovalIdIsNeverTrustedForExecution() {
        Plan clientPlan = new Plan(List.of(transferStep()));
        when(executor.execute(eq(clientPlan), anyList(), eq("tok"), eq("USER"))).thenReturn(
                new Executor.ExecResult(List.of(), List.of()));

        // An approval echo without a server-issued approvalId is rejected outright.
        assertThrows(ApprovalException.class, () -> service.chat(
                new ChatRequest(null, null, null, List.of("transfer-1"), null, null),
                "tok", "USER", "demo@bank.dev"));

        // A client plan body without a message is rejected even when approved.
        assertThrows(ApprovalException.class, () -> service.chat(
                new ChatRequest(null, null, clientPlan.steps(), List.of("transfer-1"), null, null),
                "tok", "USER", "demo@bank.dev"));

        // Nothing was ever executed from client-supplied steps.
        verify(executor, org.mockito.Mockito.never())
                .execute(any(), anyList(), any(), any());
    }

    @Test
    void approvalSessionBelongsToAnotherUserIsForbidden() {
        Plan plan = new Plan(List.of(transferStep()));
        when(planner.plan(eq("transfer 25"), any())).thenReturn(plan);
        when(executor.execute(any(), anyList(), any(), any())).thenReturn(
                new Executor.ExecResult(List.of(), List.of(transferStep())));

        AgentResponse first = service.chat(msg("transfer 25"), "tok", "USER", "demo@bank.dev");
        assertNotNull(first.approvalId());

        ApprovalException ex = assertThrows(ApprovalException.class, () -> service.chat(
                new ChatRequest(null, null, null, List.of("transfer-1"), null, first.approvalId()),
                "tok", "USER", "other@bank.dev"));
        assertEquals(ApprovalException.Kind.FORBIDDEN, ex.getKind());
    }

    @Test
    void unknownOrExpiredApprovalSessionIsRejected() {
        Plan plan = new Plan(List.of(transferStep()));
        when(planner.plan(any(), any())).thenReturn(plan);
        when(executor.execute(any(), anyList(), any(), any())).thenReturn(
                new Executor.ExecResult(List.of(), List.of(transferStep())));

        ApprovalException ex = assertThrows(ApprovalException.class, () -> service.chat(
                new ChatRequest(null, null, null, List.of("transfer-1"), null, "does-not-exist"),
                "tok", "USER", "demo@bank.dev"));
        assertEquals(ApprovalException.Kind.EXPIRED, ex.getKind());
    }

    @Test
    void employeeIsDeniedTransferTool() {
        Step transfer = transferStep();
        Plan plan = new Plan(List.of(transfer));
        when(planner.plan(eq("transfer 50"), any())).thenReturn(plan);
        when(executor.execute(eq(plan), anyList(), eq("tok"), eq("EMPLOYEE"))).thenReturn(new Executor.ExecResult(
                List.of(new StepResult("transfer-1", false, null,
                        "EMPLOYEE role cannot execute transfers - ops powers are investigation-only")), List.of()));

        AgentResponse resp = service.chat(msg("transfer 50"), "tok", "EMPLOYEE", "ops@bank.dev");

        assertTrue(resp.reply().contains("ops powers are investigation-only"), resp.reply());
    }
}