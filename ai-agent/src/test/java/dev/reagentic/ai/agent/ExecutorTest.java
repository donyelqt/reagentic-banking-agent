package dev.reagentic.ai.agent;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.mockito.ArgumentCaptor;

class ExecutorTest {

    private final AgentWorkers workers = mock(AgentWorkers.class);
    private final Executor executor = new Executor(workers);

    private static Step reconcile(String accountId) {
        return new Step("reconcile-1", "backend", "reconcileAccount",
                Map.of("accountId", accountId), List.of(), false, null);
    }

    private static Step transfer(boolean approval, String keyInArgs, String keyInStep) {
        Map<String, Object> args = new java.util.LinkedHashMap<>();
        args.put("from", "acc-checking-0001");
        args.put("to", "acc-savings-0002");
        args.put("amount", "50.00");
        if (keyInArgs != null) {
            args.put("idempotencyKey", keyInArgs);
        }
        return new Step("transfer-1", "backend", "transferFunds", args, List.of(), approval, keyInStep);
    }

    @Test
    void userIsDeniedReconcileAndWorkersNeverCalled() {
        Executor.ExecResult exec = executor.execute(new Plan(List.of(reconcile("acc-checking-0001"))),
                List.of(), "tok", "USER");

        StepResult r = exec.results().get(0);
        assertFalse(r.ok());
        assertTrue(r.error().contains("requires the EMPLOYEE (ops analyst) role"));
        verify(workers, never()).reconcile(eq("tok"), eq("acc-checking-0001"));
    }

    @Test
    void employeeIsDeniedTransfer() {
        Executor.ExecResult exec = executor.execute(new Plan(List.of(transfer(true, null, "k-1"))),
                List.of("transfer-1"), "tok", "EMPLOYEE");

        StepResult r = exec.results().get(0);
        assertFalse(r.ok());
        assertTrue(r.error().contains("ops powers are investigation-only"));
        verify(workers, never()).transfer(eq("tok"), eq("acc-checking-0001"), eq("acc-savings-0002"),
                eq("50.00"), eq("k-1"));
    }

    @Test
    void employeeTransferDeniedBeforeApprovalGate() {
        Executor.ExecResult exec = executor.execute(new Plan(List.of(transfer(true, null, "k-1"))),
                List.of(), "tok", "EMPLOYEE");

        assertTrue(exec.pending().isEmpty(), "must not wait for approval");
        assertEquals(1, exec.results().size());
        assertFalse(exec.results().get(0).ok());
        assertTrue(exec.results().get(0).error().contains("investigation-only"),
                exec.results().get(0).error());
        verify(workers, never()).transfer(eq("tok"), eq("acc-checking-0001"), eq("acc-savings-0002"),
                eq("50.00"), eq("k-1"));
    }

    @Test
    void userTransferWithoutApprovalBecomesPending() {
        Executor.ExecResult exec = executor.execute(new Plan(List.of(transfer(true, null, "k-1"))),
                List.of(), "tok", "USER");

        assertTrue(exec.results().isEmpty());
        assertEquals(1, exec.pending().size());
        assertEquals("transfer-1", exec.pending().get(0).stepId());
        verify(workers, never()).transfer(eq("tok"), eq("acc-checking-0001"), eq("acc-savings-0002"),
                eq("50.00"), eq("k-1"));
    }

    @Test
    void transferFundsNeverExecutesWhenClientPlanMarksConfirmationRequiredFalse() {
        // Trust-boundary enforcement (Story 1, criterion 4): a plan supplied by the
        // client that marks a money-movement step as confirmationRequired=false must
        // STILL require approval. Without this guard a crafted plan could move money
        // without the user ever seeing an Approve action.
        Step malicious = new Step("transfer-1", "backend", "transferFunds",
                Map.of("from", "acc-checking-0001", "to", "acc-savings-0002", "amount", "999.00"),
                List.of(), false, "k-evil-1");

        Executor.ExecResult exec = executor.execute(new Plan(List.of(malicious)), List.of(), "tok", "USER");

        assertTrue(exec.results().isEmpty(), "money must not move without approval");
        assertEquals(1, exec.pending().size());
        assertEquals("transfer-1", exec.pending().get(0).stepId());
        verify(workers, never()).transfer(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void samePlanApprovedTwiceEmitsStableIdempotencyKey() {
        // Executing the same approved plan twice must carry the same idempotency key
        // so the payment service collapses it into a single transfer (Story 1, criterion 3).
        Step t = transfer(true, null, "k-stable-1");
        Plan plan = new Plan(List.of(t));

        executor.execute(plan, List.of("transfer-1"), "tok", "USER");
        executor.execute(plan, List.of("transfer-1"), "tok", "USER");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(workers, times(2)).transfer(eq("tok"), eq("acc-checking-0001"), eq("acc-savings-0002"),
                eq("50.00"), keyCaptor.capture());
        assertEquals("k-stable-1", keyCaptor.getAllValues().get(0));
        assertEquals(keyCaptor.getAllValues().get(0), keyCaptor.getAllValues().get(1),
                "idempotency key must be stable across re-approvals");
    }

    @Test
    void approvedUserTransferUsesStepFieldIdempotencyKeyWhenArgsLackIt() {
        Executor.ExecResult exec = executor.execute(new Plan(List.of(transfer(true, null, "k-llm-1"))),
                List.of("transfer-1"), "tok", "USER");

        StepResult r = exec.results().get(0);
        assertTrue(r.ok());
        verify(workers).transfer(eq("tok"), eq("acc-checking-0001"), eq("acc-savings-0002"),
                eq("50.00"), eq("k-llm-1"));
    }

    @Test
    void approvedUserTransferPrefersArgsIdempotencyKey() {
        executor.execute(new Plan(List.of(transfer(true, "k-args", "k-step"))),
                List.of("transfer-1"), "tok", "USER");

        verify(workers).transfer(eq("tok"), eq("acc-checking-0001"), eq("acc-savings-0002"),
                eq("50.00"), eq("k-args"));
    }

    @Test
    void employeeReadsUseInternalEndpoints() {
        Step balance = new Step("s1", "backend", "getBalance",
                Map.of("accountId", "acc-checking-0001"), List.of(), false, null);
        Step txns = new Step("s2", "backend", "listTransactions",
                Map.of("accountId", "acc-savings-0002"), List.of(), false, null);
        executor.execute(new Plan(List.of(balance, txns)), List.of(), "tok", "EMPLOYEE");

        verify(workers).getBalance(eq("tok"), eq("acc-checking-0001"), eq(true));
        verify(workers).listTransactions(eq("tok"), eq("acc-savings-0002"), eq(true));
    }

    @Test
    void userReadsGoThroughOwnershipScopedEndpoints() {
        Step balance = new Step("s1", "backend", "getBalance",
                Map.of("accountId", "acc-checking-0001"), List.of(), false, null);
        executor.execute(new Plan(List.of(balance)), List.of(), "tok", "USER");

        verify(workers).getBalance(eq("tok"), eq("acc-checking-0001"), eq(false));
    }

    @Test
    void failedReconcileReturnsErrorData() {
        org.mockito.Mockito.when(workers.reconcile(eq("tok"), eq("acc-checking-0001")))
                .thenThrow(new RuntimeException("ledger unreachable"));
        Executor.ExecResult exec = executor.execute(new Plan(List.of(reconcile("acc-checking-0001"))),
                List.of(), "tok", "EMPLOYEE");

        StepResult r = exec.results().get(0);
        assertFalse(r.ok());
        assertNull(r.data());
        assertTrue(r.error().contains("ledger unreachable"));
    }

    @Test
    void reconcileDirectDelegatesForEmployee() {
        Map<String, Object> expected = Map.of("balanced", true);
        org.mockito.Mockito.when(workers.reconcile(eq("tok"), eq("acc-checking-0001"))).thenReturn(expected);

        Map<String, Object> result = executor.reconcileDirect("acc-checking-0001", "tok", "EMPLOYEE");

        assertEquals(expected, result);
        verify(workers).reconcile(eq("tok"), eq("acc-checking-0001"));
    }

    @Test
    void reconcileDirectDeniesUserBeforeWorkers() {
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> executor.reconcileDirect("acc-checking-0001", "tok", "USER"));

        verify(workers, never()).reconcile(eq("tok"), eq("acc-checking-0001"));
    }
}