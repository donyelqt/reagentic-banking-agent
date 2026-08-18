package dev.reagentic.ai.agent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatClientProviderTest {

    @SuppressWarnings("unchecked")
    private ChatClientProvider build(String provider, String apiKey) {
        ObjectProvider<GoogleGenAiChatModel> gemini = mock(ObjectProvider.class);
        when(gemini.getIfAvailable()).thenReturn(mock(GoogleGenAiChatModel.class));
        ObjectProvider<OllamaChatModel> ollama = mock(ObjectProvider.class);
        when(ollama.getIfAvailable()).thenReturn(mock(OllamaChatModel.class));
        return new ChatClientProvider(gemini, ollama, provider, apiKey);
    }

    @Test
    void geminiWithKeyWiresGeminiClient() {
        ChatClientProvider provider = build("gemini", "fake-key");
        assertTrue(provider.isGeminiConfigured(), "gemini+key should wire the Gemini ChatClient");
        assertFalse(provider.isOllamaConfigured(), "gemini provider must not wire Ollama");
    }

    @Test
    void geminiWithoutKeyFallsBackToKeywordOnly() {
        ChatClientProvider provider = build("gemini", "");
        assertFalse(provider.isGeminiConfigured(), "gemini without key must not wire a client");
        assertFalse(provider.isOllamaConfigured());
    }

    @Test
    void ollamaProviderWiresChatClient() {
        ChatClientProvider provider = build("ollama", "");
        assertTrue(provider.isOllamaConfigured(), "ollama provider should wire the Ollama ChatClient");
        assertFalse(provider.isGeminiConfigured());
    }

    @Test
    void unknownProviderWiresNothing() {
        ChatClientProvider provider = build("bogus", "any");
        assertFalse(provider.isGeminiConfigured());
        assertFalse(provider.isOllamaConfigured());
    }
}
