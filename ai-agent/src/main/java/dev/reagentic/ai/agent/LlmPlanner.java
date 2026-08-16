package dev.reagentic.ai.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.reagentic.common.DemoConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Primary planner: turns a natural-language request into a plan DAG via an LLM.
 * The provider is selected with {@code SPRING_AI_PROVIDER}:
 * <ul>
 *   <li>{@code gemini} (default) — Google Gemini REST API
 *       ({@code SPRING_AI_GEMINI_API_KEY}, {@code SPRING_AI_GEMINI_MODEL})</li>
 *   <li>{@code ollama} — local Ollama via Spring AI
 *       ({@code SPRING_AI_OLLAMA_BASE_URL}, {@code SPRING_AI_OLLAMA_MODEL})</li>
 * </ul>
 *
 * The LLM is the primary path, but the deterministic {@link KeywordPlanner} is
 * the MANDATORY safety net: any failure (model unreachable, missing api key,
 * unparseable JSON, an unknown/invalid tool, a malformed transfer) delegates
 * to it so the demo hero flows never hard-fail.
 */
@Component
@Primary
public class LlmPlanner implements Planner {

    private static final Logger log = LoggerFactory.getLogger(LlmPlanner.class);

    private static final Set<String> TOOLS = Set.of(
            "listAccounts", "getBalance", "listTransactions", "transferFunds", "reconcileAccount");

    private final KeywordPlanner keywordPlanner;
    private final ChatClient chatClient;
    private final GeminiApi geminiApi;
    private final String provider;

    public LlmPlanner(ObjectProvider<OllamaChatModel> ollama,
                      KeywordPlanner keywordPlanner,
                      @Value("${SPRING_AI_PROVIDER:gemini}") String provider,
                      @Value("${SPRING_AI_GEMINI_API_KEY:}") String geminiApiKey,
                      @Value("${SPRING_AI_GEMINI_MODEL:gemini-2.5-flash}") String geminiModel,
                      @Value("${SPRING_AI_GEMINI_BASE_URL:https://generativelanguage.googleapis.com/v1beta}") String geminiBaseUrl,
                      ObjectMapper objectMapper) {
        this.keywordPlanner = keywordPlanner;
        this.provider = provider == null ? "gemini" : provider.trim().toLowerCase();
        if ("gemini".equals(this.provider)) {
            if (geminiApiKey == null || geminiApiKey.isBlank()) {
                this.geminiApi = null;
                this.chatClient = null;
                log.warn("SPRING_AI_PROVIDER=gemini but SPRING_AI_GEMINI_API_KEY is not set - "
                        + "using the deterministic keyword planner only");
            } else {
                this.geminiApi = new GeminiApi(geminiBaseUrl, geminiModel, geminiApiKey, objectMapper);
                this.chatClient = null;
                log.info("LLM planner using provider 'gemini' (model {})", geminiModel);
            }
        } else if ("ollama".equals(this.provider)) {
            OllamaChatModel model = ollama.getIfAvailable();
            if (model == null) {
                this.geminiApi = null;
                this.chatClient = null;
                log.warn("SPRING_AI_PROVIDER=ollama but no Ollama model is configured - "
                        + "using the deterministic keyword planner only");
            } else {
                this.geminiApi = null;
                this.chatClient = ChatClient.create(model);
                log.info("LLM planner using provider 'ollama'");
            }
        } else {
            this.geminiApi = null;
            this.chatClient = null;
            log.warn("Unknown SPRING_AI_PROVIDER '{}' - using the deterministic keyword planner only", this.provider);
        }
    }

    @Override
    public Plan plan(String message) {
        if (message == null || message.isBlank()) {
            return new Plan(List.of());
        }
        try {
            PlanDto dto = callLlm(message);
            Plan plan = toPlan(dto);
            if (plan == null || plan.steps().isEmpty()) {
                return keywordPlanner.plan(message);
            }
            return plan;
        } catch (Exception e) {
            log.warn("LLM plan failed ({}), using keyword planner: {}", provider, e.getMessage());
            return keywordPlanner.plan(message);
        }
    }

    private PlanDto callLlm(String message) {
        if ("gemini".equals(provider)) {
            String text = geminiApi.chat(SYSTEM_PROMPT + "\n\nUser request: " + message);
            try {
                return geminiApi.objectMapper.readValue(text, PlanDto.class);
            } catch (Exception e) {
                throw new RuntimeException("gemini returned unparseable plan: " + e.getMessage(), e);
            }
        }
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(u -> u.text("User request: {msg}").param("msg", message))
                .call()
                .entity(PlanDto.class);
    }

    private Plan toPlan(PlanDto dto) {
        if (dto == null || dto.steps() == null) {
            return null;
        }
        List<Step> steps = new ArrayList<>();
        int i = 0;
        for (StepDto s : dto.steps()) {
            if (s == null || s.tool() == null || !TOOLS.contains(s.tool())) {
                return null; // one invalid step invalidates the whole plan -> fall back
            }
            Map<String, Object> args = s.args() == null ? new HashMap<>() : new HashMap<>(s.args());
            normalizeAccounts(args);
            String idem = s.idempotencyKey();
            if ("transferFunds".equals(s.tool())) {
                if (!args.containsKey("from") || !args.containsKey("to") || !args.containsKey("amount")) {
                    return null;
                }
                if (idem == null || idem.isBlank()) {
                    idem = UUID.randomUUID().toString();
                }
            }
            steps.add(new Step(
                    (s.stepId() == null || s.stepId().isBlank()) ? "s" + (i++) : s.stepId(),
                    "backend",
                    s.tool(),
                    args,
                    s.dependsOn() == null ? List.of() : s.dependsOn(),
                    s.confirmationRequired(),
                    idem));
        }
        return new Plan(steps);
    }

    private void normalizeAccounts(Map<String, Object> args) {
        for (String key : new String[]{"accountId", "from", "to"}) {
            Object v = args.get(key);
            if (v instanceof String s) {
                String low = s.toLowerCase();
                if (low.contains("savings")) {
                    args.put(key, DemoConstants.SAVINGS_ACCOUNT_ID);
                } else if (low.contains("checking")) {
                    args.put(key, DemoConstants.CHECKING_ACCOUNT_ID);
                }
            }
        }
    }

    private static final String SYSTEM_PROMPT = """
            You are the planning module of a banking assistant. Given a user request, output a JSON plan.
            Available tools (the "tool" field must be exactly one of):
              - listAccounts: no args. Lists the user's accounts.
              - getBalance: args {"accountId"}. Use "checking" or "savings" if the user refers by type.
              - listTransactions: args {"accountId"}. Use "checking" or "savings".
              - transferFunds: args {"from","to","amount"}. Use "checking"/"savings" for from/to; amount is a decimal string. Set confirmationRequired=true.
              - reconcileAccount: args {"accountId"}. Use "checking" or "savings".
            Rules:
              - For a transfer, set confirmationRequired=true and provide a unique idempotencyKey string.
              - stepId must be unique per step (e.g. "transfer-1", "reconcile-1", "s1").
              - dependsOn is a list of stepIds that must run first (omit if none).
              - Output ONLY the plan object; it will be parsed automatically.
            """;

    /**
     * Minimal Google Gemini REST client (generativelanguage.googleapis.com).
     * Uses the same JSON-first pattern as the rest of the agent; no extra
     * Spring AI module required (the Gemini starter needs Spring AI 1.1+).
     */
    private static final class GeminiApi {

        private final RestClient client;
        private final ObjectMapper objectMapper;
        private final String model;
        private final String apiKey;

        GeminiApi(String baseUrl, String model, String apiKey, ObjectMapper objectMapper) {
            this.model = model;
            this.apiKey = apiKey;
            this.objectMapper = objectMapper;
            this.client = RestClient.builder().baseUrl(baseUrl).build();
        }

        String chat(String prompt) {
            String body = """
                    {"contents":[{"role":"user","parts":[{"text":"%s"}]}],
                     "generationConfig":{"temperature":0.0,"responseMimeType":"application/json"}}
                    """.formatted(escape(prompt));
            String resp = client.post()
                    .uri("/models/{model}:generateContent?key={key}", model, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return extractText(resp);
        }

        private String extractText(String resp) {
            JsonNode root;
            try {
                root = objectMapper.readTree(resp);
            } catch (Exception e) {
                throw new RuntimeException("gemini returned unparseable response: " + e.getMessage(), e);
            }
            JsonNode text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (text.isMissingNode() || text.isNull()) {
                throw new RuntimeException("gemini returned no text: "
                        + root.path("promptFeedback").path("blockReason").asText("unknown reason"));
            }
            return text.asText();
        }

        private String escape(String s) {
            return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
        }
    }
}