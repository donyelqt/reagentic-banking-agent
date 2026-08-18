package dev.reagentic.ai.agent;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmPlannerTest {

    private static final KeywordPlanner KEYWORD = new KeywordPlanner();

    private LlmPlanner build(ChatClientProvider provider) {
        return new LlmPlanner(KEYWORD, provider);
    }

    @Test
    void blankMessageReturnsEmptyPlan() {
        ChatClientProvider provider = mock(ChatClientProvider.class);
        LlmPlanner planner = build(provider);

        Plan plan = planner.plan("", List.of());

        assertTrue(plan.steps().isEmpty());
    }

    @Test
    void noClientConfiguredFallsBackToKeywordPlanner() {
        ChatClientProvider provider = mock(ChatClientProvider.class);
        when(provider.client()).thenReturn(null);
        LlmPlanner planner = build(provider);

        Plan plan = planner.plan("reconcile checking", List.of());

        assertEquals(KEYWORD.plan("reconcile checking", List.of()), plan);
    }

    @Test
    void noClientConfiguredIgnoresHistoryLikeKeywordPlanner() {
        ChatClientProvider provider = mock(ChatClientProvider.class);
        when(provider.client()).thenReturn(null);
        LlmPlanner planner = build(provider);

        Plan withHistory = planner.plan("what's my balance", List.of("User: hi", "Agent: hello"));
        Plan withoutHistory = planner.plan("what's my balance", List.of());

        assertEquals(withoutHistory, withHistory);
    }

    @Test
    void testingSeamsDelegateToChatClientProvider() {
        ChatClientProvider provider = mock(ChatClientProvider.class);
        when(provider.isGeminiConfigured()).thenReturn(true);
        when(provider.isOllamaConfigured()).thenReturn(false);
        LlmPlanner planner = build(provider);

        assertTrue(planner.isGeminiConfigured());
        assertFalse(planner.isOllamaConfigured());
    }

    @Test
    void toPlanForcesApprovalOnTransfersEvenWhenLlmOmitsIt() {
        LlmPlanner planner = build(mock(ChatClientProvider.class));
        StepDto transfer = new StepDto("transfer-1", "transferFunds",
                Map.of("from", "checking", "to", "savings", "amount", "50.00"),
                List.of(), false, null);

        Plan plan = planner.toPlan(new PlanDto(List.of(transfer)));

        Step step = plan.steps().get(0);
        assertTrue(step.confirmationRequired());
        assertNotNull(step.idempotencyKey());
        assertFalse(step.idempotencyKey().isBlank());
    }

    @Test
    void toPlanPreservesLlmConfirmationForNonTransferTools() {
        LlmPlanner planner = build(mock(ChatClientProvider.class));
        StepDto reconcile = new StepDto("reconcile-1", "reconcileAccount",
                Map.of("accountId", "checking"), List.of(), false, null);
        StepDto readOnly = new StepDto("s1", "getBalance",
                Map.of("accountId", "checking"), List.of(), true, null);

        Plan plan = planner.toPlan(new PlanDto(List.of(reconcile, readOnly)));

        assertFalse(plan.steps().get(0).confirmationRequired());
        assertTrue(plan.steps().get(1).confirmationRequired());
    }

    @Test
    void toPlanRejectsTransferMissingRequiredArgs() {
        LlmPlanner planner = build(mock(ChatClientProvider.class));
        StepDto transfer = new StepDto("transfer-1", "transferFunds",
                Map.of("from", "checking"), List.of(), true, null);

        assertNull(planner.toPlan(new PlanDto(List.of(transfer))));
    }

    @Test
    void toPlanRejectsUnknownTool() {
        LlmPlanner planner = build(mock(ChatClientProvider.class));
        StepDto rogue = new StepDto("s1", "deleteAccount", Map.of(), List.of(), false, null);

        assertNull(planner.toPlan(new PlanDto(List.of(rogue))));
    }
}
