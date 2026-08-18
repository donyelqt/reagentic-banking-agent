package dev.reagentic.ai.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Primary transaction classifier: assigns each transaction a {@link SpendingCategory}
 * via a single batched Gemini call, prompt-engineered with the fixed category taxonomy,
 * few-shot examples, and a strict "one category per input, same order" JSON contract.
 *
 * The LLM is the primary path, but {@link KeywordTransactionClassifier} is the MANDATORY
 * safety net. Trust is verified per item: each response entry must carry a known category
 * AND a description matching the corresponding input, so a response that is reordered,
 * reworded, or partially malformed can never silently mislabel money. Items the LLM
 * drifted on are keyword-classified individually; only a response whose shape cannot be
 * trusted at all (missing/unparseable JSON, wrong item count) delegates the WHOLE batch,
 * mirroring the fallback contract {@link LlmPlanner} already guarantees for planning.
 */
@Component
@Primary
public class LlmTransactionClassifier implements TransactionClassifier {

    private static final Logger log = LoggerFactory.getLogger(LlmTransactionClassifier.class);

    private final KeywordTransactionClassifier keywordClassifier;
    private final ChatClientProvider chatClientProvider;

    public LlmTransactionClassifier(KeywordTransactionClassifier keywordClassifier,
                                    ChatClientProvider chatClientProvider) {
        this.keywordClassifier = keywordClassifier;
        this.chatClientProvider = chatClientProvider;
    }

    @Override
    public List<ClassifiedTransaction> classify(List<TransactionInput> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return List.of();
        }
        ChatClient client = chatClientProvider.client();
        if (client == null) {
            return keywordClassifier.classify(transactions);
        }
        try {
            ClassificationDto dto = callLlm(client, transactions);
            List<ClassifiedTransaction> result = toClassified(dto, transactions);
            if (result == null) {
                return keywordClassifier.classify(transactions);
            }
            return result;
        } catch (Exception e) {
            log.warn("LLM classification failed ({}), using keyword classifier: {}",
                    chatClientProvider.provider(), e.getMessage());
            return keywordClassifier.classify(transactions);
        }
    }

    private ClassificationDto callLlm(ChatClient client, List<TransactionInput> transactions) {
        StringBuilder user = new StringBuilder("Classify these ").append(transactions.size())
                .append(" transactions, in order:\n");
        int i = 1;
        for (TransactionInput t : transactions) {
            user.append(i++).append(". description=\"").append(t.description())
                    .append("\" amount=").append(t.amount()).append("\n");
        }
        return client.prompt()
                .system(SYSTEM_PROMPT)
                .user(user.toString())
                .call()
                .entity(ClassificationDto.class);
    }

    /**
     * Maps the LLM response onto the input 1:1. Returns null (triggering whole-batch
     * fallback) only when the response shape is untrustworthy: missing JSON or a
     * different item count. Per-item problems (unknown category, reordered or reworded
     * description) fall back individually to the keyword classifier so one drifted
     * entry can never corrupt the rest of the batch.
     */
    private List<ClassifiedTransaction> toClassified(ClassificationDto dto, List<TransactionInput> transactions) {
        if (dto == null || dto.items() == null || dto.items().size() != transactions.size()) {
            return null;
        }
        ClassifiedTransaction[] result = new ClassifiedTransaction[transactions.size()];
        int fallbacks = 0;
        for (int i = 0; i < transactions.size(); i++) {
            ClassifiedTransaction classified = toOne(dto.items().get(i), transactions.get(i));
            if (classified == null) {
                fallbacks++;
                result[i] = keywordClassifier.classify(List.of(transactions.get(i))).get(0);
            } else {
                result[i] = classified;
            }
        }
        if (fallbacks > 0) {
            log.warn("LLM classification drifted on {}/{} items (unknown category or order mismatch), " +
                    "keyword-classified only those", fallbacks, transactions.size());
        }
        return Arrays.asList(result);
    }

    /** A single response entry is trusted only if it names a known category AND echoes the input description. */
    private ClassifiedTransaction toOne(ClassificationItemDto item, TransactionInput input) {
        if (item == null || item.category() == null || item.description() == null || input == null) {
            return null;
        }
        SpendingCategory category;
        try {
            category = SpendingCategory.valueOf(item.category().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
        String expected = input.description() == null ? "" : input.description().trim();
        if (!item.description().trim().equalsIgnoreCase(expected)) {
            return null;
        }
        return new ClassifiedTransaction(input.description(), input.amount(), category);
    }

    private static final String SYSTEM_PROMPT = """
            You are the transaction classification module of a banking assistant. Given a numbered
            list of transactions (description and signed amount, negative = money out), output a JSON
            object with an "items" array containing exactly one entry per transaction, IN THE SAME ORDER.
            Each entry has "description" (copy it verbatim) and "category", which must be exactly one of:
              GROCERIES, DINING, TRANSPORT, UTILITIES, SUBSCRIPTIONS, SHOPPING, ENTERTAINMENT, HEALTH,
              TRAVEL, INCOME, TRANSFER, OTHER
            Examples:
              "Starbucks Coffee" -> DINING
              "Netflix Monthly" -> SUBSCRIPTIONS
              "Grab Ride" -> TRANSPORT
              "SM Supermarket" -> GROCERIES
              "Meralco Bill" -> UTILITIES
              "Payroll Deposit" (positive amount) -> INCOME
              "Transfer to Savings" -> TRANSFER
            If nothing fits, use OTHER. Output ONLY the JSON object; it will be parsed automatically.
            """;
}
