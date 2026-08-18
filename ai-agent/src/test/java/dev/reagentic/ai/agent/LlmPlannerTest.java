package dev.reagentic.ai.agent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
