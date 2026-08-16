package dev.reagentic.ai.agent;

import dev.reagentic.common.DemoConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Primary planner: turns a natural-language request into a plan DAG via an LLM.
 * The provider is selected with {@code agent.provider}:
 * <ul>
 *   <li>{@code gemini} (default) - Spring AI Google GenAI starter
 *       ({@code spring.ai.google.genai.*}, key via {@code AGENT_GEMINI_API_KEY})</li>
 *   <li>{@code ollama} - Spring AI Ollama starter ({@code spring.ai.ollama.*})</li>
 * </ul>
 *
 * Both starter autoconfigs are active by default ({@code spring.ai.model.chat}
 * is left unset, so each {@code matchIfMissing=true} conditional applies); the
 * planner selects the provider with {@link ObjectProvider}. A missing Gemini
 * API key is handled by a non-blank placeholder in application.yml so the
 * autoconfig does not fail the context at boot; this planner then checks the
 * real key property and falls back to the deterministic {@link KeywordPlanner}.
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
    private final ChatClient geminiChatClient;
    private final ChatClient ollamaChatClient;
    private final String provider;

    public LlmPlanner(ObjectProvider<GoogleGenAiChatModel> gemini,
                      ObjectProvider<OllamaChatModel> ollama,
                      KeywordPlanner keywordPlanner,
                      @Value("${agent.provider:gemini}") String provider,
                      @Value("${agent.gemini.api-key:}") String geminiApiKey) {
        this.keywordPlanner = keywordPlanner;
        this.provider = provider == null ? "gemini" : provider.trim().toLowerCase();
        if ("gemini".equals(this.provider)) {
            if (geminiApiKey == null || geminiApiKey.isBlank()) {
                this.geminiChatClient = null;
                this.ollamaChatClient = null;
                log.warn("agent.provider=gemini but agent.gemini.api-key is not set - "
                        + "using the deterministic keyword planner only");
            } else {
                GoogleGenAiChatModel model = gemini.getIfAvailable();
                if (model == null) {
                    this.geminiChatClient = null;
                    this.ollamaChatClient = null;
                    log.warn("agent.provider=gemini but no GoogleGenAiChatModel bean is available - "
                            + "using the deterministic keyword planner only");
                } else {
                    this.geminiChatClient = ChatClient.create(model);
                    this.ollamaChatClient = null;
                    log.info("LLM planner using provider 'gemini' (Spring AI Google GenAI)");
                }
            }
        } else if ("ollama".equals(this.provider)) {
            OllamaChatModel model = ollama.getIfAvailable();
            if (model == null) {
                this.geminiChatClient = null;
                this.ollamaChatClient = null;
                log.warn("agent.provider=ollama but no OllamaChatModel bean is available "
                        + "(enable it with spring.ai.model.chat=ollama and set "
                        + "spring.ai.ollama.chat.model) - using the deterministic keyword planner only");
            } else {
                this.geminiChatClient = null;
                this.ollamaChatClient = ChatClient.create(model);
                log.info("LLM planner using provider 'ollama'");
            }
        } else {
            this.geminiChatClient = null;
            this.ollamaChatClient = null;
            log.warn("Unknown agent.provider '{}' - using the deterministic keyword planner only", this.provider);
        }
    }

    /** Testing seam: true when the Gemini ChatClient was wired. */
    boolean isGeminiConfigured() {
        return geminiChatClient != null;
    }

    /** Testing seam: true when the Ollama ChatClient was wired. */
    boolean isOllamaConfigured() {
        return ollamaChatClient != null;
    }

    @Override
    public Plan plan(String message) {
        if (message == null || message.isBlank()) {
            return new Plan(List.of());
        }
        if (geminiChatClient == null && ollamaChatClient == null) {
            return keywordPlanner.plan(message);
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
        ChatClient client = "gemini".equals(provider) ? geminiChatClient : ollamaChatClient;
        return client.prompt()
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
}