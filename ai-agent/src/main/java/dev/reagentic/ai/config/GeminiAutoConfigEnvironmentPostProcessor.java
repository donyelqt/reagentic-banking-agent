package dev.reagentic.ai.config;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Spring AI's {@code GoogleGenAiChatAutoConfiguration} eagerly builds a
 * {@code com.google.genai.Client} singleton at context startup and throws if
 * {@code spring.ai.google.genai.api-key} is blank - before {@link
 * dev.reagentic.ai.agent.ChatClientProvider}'s own "no key configured, fall
 * back" logic ever runs. {@code infra/.env.example} promises the app still
 * works with {@code AGENT_GEMINI_API_KEY} left blank, so when no key is
 * configured this disables the Google GenAI chat autoconfiguration entirely
 * (the documented {@code spring.ai.model.chat=none} switch) instead of
 * letting it crash the whole service. Runs as an {@link
 * EnvironmentPostProcessor} because {@code @ConditionalOnProperty} on the
 * autoconfiguration class is evaluated before any regular {@code @Bean}
 * could set this.
 */
public class GeminiAutoConfigEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String apiKey = environment.getProperty("agent.gemini.api-key", "");
        if (apiKey.isBlank() && environment.getProperty("spring.ai.model.chat") == null) {
            environment.getPropertySources().addFirst(new MapPropertySource(
                    "gemini-disabled-when-unconfigured", Map.of("spring.ai.model.chat", "none")));
        }
    }
}
