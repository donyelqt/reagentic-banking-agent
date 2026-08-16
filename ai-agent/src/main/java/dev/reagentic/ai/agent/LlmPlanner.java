package dev.reagentic.ai.agent;

import dev.reagentic.common.DemoConstants;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Primary planner: turns a natural-language request into a plan DAG via a
 * Spring AI {@link ChatClient} backed by local Ollama (default model is
 * configurable via SPRING_AI_OLLAMA_MODEL / spring.ai.ollama.chat.model).
 *
 * The LLM is the primary path, but the deterministic {@link KeywordPlanner} is
 * the MANDATORY safety net: any failure (model unreachable, unparseable JSON,
 * an unknown/invalid tool, a malformed transfer) delegates to it so the demo
 * hero flows never hard-fail. An OpenAI-backed ChatModel can be wired in later
 * by selecting it here when SPRING_AI_OPENAI_API_KEY is present.
 */
@Component
@Primary
public class LlmPlanner implements Planner {

    private static final Set<String> TOOLS = Set.of(
            "listAccounts", "getBalance", "listTransactions", "transferFunds", "reconcileAccount");

    private final ChatClient chatClient;
    private final KeywordPlanner keywordPlanner;

    public LlmPlanner(OllamaChatModel ollama, KeywordPlanner keywordPlanner) {
        this.keywordPlanner = keywordPlanner;
        this.chatClient = ChatClient.create(ollama);
    }

    @Override
    public Plan plan(String message) {
        if (message == null || message.isBlank()) {
            return new Plan(List.of());
        }
        try {
            PlanDto dto = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(u -> u.text("User request: {msg}").param("msg", message))
                    .call()
                    .entity(PlanDto.class);
            Plan plan = toPlan(dto);
            if (plan == null || plan.steps().isEmpty()) {
                return keywordPlanner.plan(message);
            }
            return plan;
        } catch (Exception e) {
            return keywordPlanner.plan(message);
        }
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