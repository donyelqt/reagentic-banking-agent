package dev.reagentic.ai.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resolves the single active {@link ChatClient} for the configured {@code agent.provider}
 * (gemini or ollama), shared by every Gemini-backed feature ({@link LlmPlanner} planning,
 * {@link LlmTransactionClassifier} classification) so provider selection and the
 * "no client available" fallback behavior live in exactly one place.
 */
@Component
public class ChatClientProvider {

    private static final Logger log = LoggerFactory.getLogger(ChatClientProvider.class);

    private final ChatClient geminiChatClient;
    private final ChatClient ollamaChatClient;
    private final String provider;

    public ChatClientProvider(ObjectProvider<GoogleGenAiChatModel> gemini,
                              ObjectProvider<OllamaChatModel> ollama,
                              @Value("${agent.provider:gemini}") String provider,
                              @Value("${agent.gemini.api-key:}") String geminiApiKey) {
        this.provider = provider == null ? "gemini" : provider.trim().toLowerCase();
        if ("gemini".equals(this.provider)) {
            if (geminiApiKey == null || geminiApiKey.isBlank()) {
                this.geminiChatClient = null;
                this.ollamaChatClient = null;
                log.warn("agent.provider=gemini but agent.gemini.api-key is not set - "
                        + "Gemini-backed features will use their deterministic fallback only");
            } else {
                GoogleGenAiChatModel model = gemini.getIfAvailable();
                if (model == null) {
                    this.geminiChatClient = null;
                    this.ollamaChatClient = null;
                    log.warn("agent.provider=gemini but no GoogleGenAiChatModel bean is available - "
                            + "Gemini-backed features will use their deterministic fallback only");
                } else {
                    this.geminiChatClient = ChatClient.create(model);
                    this.ollamaChatClient = null;
                    log.info("Chat client wired for provider 'gemini' (Spring AI Google GenAI)");
                }
            }
        } else if ("ollama".equals(this.provider)) {
            OllamaChatModel model = ollama.getIfAvailable();
            if (model == null) {
                this.geminiChatClient = null;
                this.ollamaChatClient = null;
                log.warn("agent.provider=ollama but no OllamaChatModel bean is available "
                        + "(enable it with spring.ai.model.chat=ollama and set "
                        + "spring.ai.ollama.chat.model) - Gemini-backed features will use their "
                        + "deterministic fallback only");
            } else {
                this.geminiChatClient = null;
                this.ollamaChatClient = ChatClient.create(model);
                log.info("Chat client wired for provider 'ollama'");
            }
        } else {
            this.geminiChatClient = null;
            this.ollamaChatClient = null;
            log.warn("Unknown agent.provider '{}' - Gemini-backed features will use their "
                    + "deterministic fallback only", this.provider);
        }
    }

    /** The active ChatClient for the configured provider, or null if none is wired. */
    public ChatClient client() {
        return "gemini".equals(provider) ? geminiChatClient : ollamaChatClient;
    }

    public String provider() {
        return provider;
    }

    /** Testing seam: true when the Gemini ChatClient was wired. */
    boolean isGeminiConfigured() {
        return geminiChatClient != null;
    }

    /** Testing seam: true when the Ollama ChatClient was wired. */
    boolean isOllamaConfigured() {
        return ollamaChatClient != null;
    }
}
