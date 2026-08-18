package dev.reagentic.ai.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Primary transaction classifier: assigns each transaction a {@link SpendingCategory}
 * via a single batched Gemini call, prompt-engineered with the fixed category taxonomy,
 * few-shot examples, and a strict "one category per input, same order" JSON contract.
 *
 * The LLM is the primary path, but {@link KeywordTransactionClassifier} is the MANDATORY
 * safety net: any failure (model unreachable, missing api key, unparseable JSON, a
 * response whose item count doesn't match the input, or an unknown category value)
 * delegates the WHOLE batch to it, mirroring the fallback contract {@link LlmPlanner}
 * already guarantees for planning.
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

    /** Returns null (triggering fallback) if the response doesn't line up with the input 1:1. */
    private List<ClassifiedTransaction> toClassified(ClassificationDto dto, List<TransactionInput> transactions) {
        if (dto == null || dto.items() == null || dto.items().size() != transactions.size()) {
            return null;
        }
        List<ClassifiedTransaction> result = new ArrayList<>();
        for (int i = 0; i < transactions.size(); i++) {
            ClassificationItemDto item = dto.items().get(i);
            if (item == null || item.category() == null) {
                return null;
            }
            SpendingCategory category;
            try {
                category = SpendingCategory.valueOf(item.category().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null; // one invalid category invalidates the whole batch -> fall back
            }
            result.add(new ClassifiedTransaction(transactions.get(i).description(), transactions.get(i).amount(), category));
        }
        return result;
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
