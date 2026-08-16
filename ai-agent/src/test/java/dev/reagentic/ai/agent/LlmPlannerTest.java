package dev.reagentic.ai.agent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmPlannerTest {

    private static final KeywordPlanner KEYWORD = new KeywordPlanner();

    @SuppressWarnings("unchecked")
    private LlmPlanner build(String provider, String apiKey) {
        ObjectProvider<GoogleGenAiChatModel> gemini = mock(ObjectProvider.class);
        when(gemini.getIfAvailable()).thenReturn(mock(GoogleGenAiChatModel.class));
        ObjectProvider<OllamaChatModel> ollama = mock(ObjectProvider.class);
        when(ollama.getIfAvailable()).thenReturn(mock(OllamaChatModel.class));
        return new LlmPlanner(gemini, ollama, KEYWORD, provider, apiKey);
    }

    @Test
    void geminiWithKeyWiresGeminiClient() {
        LlmPlanner planner = build("gemini", "fake-key");
        assertTrue(planner.isGeminiConfigured(), "gemini+key should wire the Gemini ChatClient");
        assertFalse(planner.isOllamaConfigured(), "gemini provider must not wire Ollama");
    }

    @Test
    void geminiWithoutKeyFallsBackToKeywordOnly() {
        LlmPlanner planner = build("gemini", "");
        assertFalse(planner.isGeminiConfigured(), "gemini without key must not wire a client");
        assertFalse(planner.isOllamaConfigured());
    }

    @Test
    void ollamaProviderWiresChatClient() {
        LlmPlanner planner = build("ollama", "");
        assertTrue(planner.isOllamaConfigured(), "ollama provider should wire the Ollama ChatClient");
        assertFalse(planner.isGeminiConfigured());
    }

    @Test
    void unknownProviderWiresNothing() {
        LlmPlanner planner = build("bogus", "any");
        assertFalse(planner.isGeminiConfigured());
        assertFalse(planner.isOllamaConfigured());
    }
}