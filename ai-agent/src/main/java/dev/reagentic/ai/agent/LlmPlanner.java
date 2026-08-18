package dev.reagentic.ai.agent;

import dev.reagentic.common.DemoConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
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
 * The provider is resolved by {@link ChatClientProvider} from {@code agent.provider}:
 * <ul>
 *   <li>{@code gemini} (default) - Spring AI Google GenAI starter
 *       ({@code spring.ai.google.genai.*}, key via {@code AGENT_GEMINI_API_KEY})</li>
 *   <li>{@code ollama} - Spring AI Ollama starter ({@code spring.ai.ollama.*})</li>
 * </ul>
 *
 * The LLM is the primary path, but the deterministic {@link KeywordPlanner} is
 * the MANDATORY safety net: any failure (model unreachable, missing api key,
 * unparseable JSON, an unknown/invalid tool, a malformed transfer) delegates
 * to it so the demo hero flows never hard-fail.
 *
 * Context management: {@link #plan(String, List)} accepts the prior conversation
 * turns and folds a bounded window of them into the prompt so the LLM can resolve
 * follow-ups like "do the same for savings". The window is capped at
 * {@link #HISTORY_WINDOW} lines to bound prompt size/cost as a chat grows.
 */
@Component
@Primary
public class LlmPlanner implements Planner {

    private static final Logger log = LoggerFactory.getLogger(LlmPlanner.class);

    private static final Set<String> TOOLS = Set.of(
            "listAccounts", "getBalance", "listTransactions", "transferFunds", "reconcileAccount");

    private static final int HISTORY_WINDOW = 6;

    private static final int MAX_HISTORY_LINE_LENGTH = 500;

    private final KeywordPlanner keywordPlanner;
    private final ChatClientProvider chatClientProvider;

    public LlmPlanner(KeywordPlanner keywordPlanner, ChatClientProvider chatClientProvider) {
        this.keywordPlanner = keywordPlanner;
        this.chatClientProvider = chatClientProvider;
    }

    /** Testing seam: true when the Gemini ChatClient was wired. */
    boolean isGeminiConfigured() {
        return chatClientProvider.isGeminiConfigured();
    }

    /** Testing seam: true when the Ollama ChatClient was wired. */
    boolean isOllamaConfigured() {
        return chatClientProvider.isOllamaConfigured();
    }

    @Override
    public Plan plan(String message, List<String> history) {
        if (message == null || message.isBlank()) {
            return new Plan(List.of());
        }
        ChatClient client = chatClientProvider.client();
        if (client == null) {
            return keywordPlanner.plan(message, history);
        }
        try {
            PlanDto dto = callLlm(client, message, history);
            Plan plan = toPlan(dto);
            if (plan == null || plan.steps().isEmpty()) {
                return keywordPlanner.plan(message, history);
            }
            return plan;
        } catch (Exception e) {
            log.warn("LLM plan failed ({}), using keyword planner: {}", chatClientProvider.provider(), e.getMessage());
            return keywordPlanner.plan(message, history);
        }
    }

    private PlanDto callLlm(ChatClient client, String message, List<String> history) {
        String userText = withHistory(message, history);
        return client.prompt()
                .system(SYSTEM_PROMPT)
                .user(userText)
                .call()
                .entity(PlanDto.class);
    }

    /** Prepends a bounded window of prior turns as conversation context ahead of the current request. */
    private String withHistory(String message, List<String> history) {
        if (history == null || history.isEmpty()) {
            return "User request: " + message;
        }
        int from = Math.max(0, history.size() - HISTORY_WINDOW);
        StringBuilder sb = new StringBuilder("Conversation so far:\n");
        for (String line : history.subList(from, history.size())) {
            if (line != null && !line.isBlank()) {
                sb.append(line, 0, Math.min(line.length(), MAX_HISTORY_LINE_LENGTH)).append("\n");
            }
        }
        sb.append("User request: ").append(message);
        return sb.toString();
    }

    Plan toPlan(PlanDto dto) {
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
            boolean confirmationRequired = s.confirmationRequired();
            if ("transferFunds".equals(s.tool())) {
                if (!args.containsKey("from") || !args.containsKey("to") || !args.containsKey("amount")) {
                    return null;
                }
                // Enforced invariant: money movement always waits for explicit approval,
                // regardless of what the LLM returned. The approval gate is a backend
                // guarantee, not a model suggestion.
                confirmationRequired = true;
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
                    confirmationRequired,
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
              - The user message may be preceded by "Conversation so far:" with prior turns. Use it only to
                resolve references like "do the same" or "that account" - never repeat a previously approved
                or executed action unless the current request asks for it again.
              - Output ONLY the plan object; it will be parsed automatically.
            """;
}
