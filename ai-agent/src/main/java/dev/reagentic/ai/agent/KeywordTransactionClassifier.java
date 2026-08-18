package dev.reagentic.ai.agent;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic, LLM-free transaction classifier. Mandatory safety net: the agent
 * always returns a category for every transaction even when no LLM is available,
 * mirroring the role {@link KeywordPlanner} plays for planning.
 */
@Component
public class KeywordTransactionClassifier implements TransactionClassifier {

    private static final Map<SpendingCategory, List<String>> KEYWORDS = new LinkedHashMap<>();

    static {
        KEYWORDS.put(SpendingCategory.DINING, List.of(
                "restaurant", "cafe", "coffee", "starbucks", "mcdo", "mcdonald", "jollibee",
                "diner", "bar", "grill", "eatery", "pizza", "food"));
        KEYWORDS.put(SpendingCategory.GROCERIES, List.of(
                "grocery", "groceries", "supermarket", "sm market", "puregold", "market"));
        KEYWORDS.put(SpendingCategory.SUBSCRIPTIONS, List.of(
                "netflix", "spotify", "subscription", "prime video", "disney+", "hbo", "youtube premium"));
        KEYWORDS.put(SpendingCategory.TRANSPORT, List.of(
                "uber", "grab", "taxi", "gas", "fuel", "petron", "shell", "parking", "toll", "mrt", "lrt"));
        KEYWORDS.put(SpendingCategory.UTILITIES, List.of(
                "electric", "meralco", "water", "maynilad", "internet", "pldt", "globe", "utility", "utilities"));
        KEYWORDS.put(SpendingCategory.SHOPPING, List.of(
                "shopee", "lazada", "amazon", "mall", "shopping", "store"));
        KEYWORDS.put(SpendingCategory.ENTERTAINMENT, List.of(
                "cinema", "movie", "concert", "game", "steam", "playstation"));
        KEYWORDS.put(SpendingCategory.HEALTH, List.of(
                "pharmacy", "clinic", "hospital", "doctor", "mercury drug", "watsons"));
        KEYWORDS.put(SpendingCategory.TRAVEL, List.of(
                "airline", "hotel", "booking.com", "airbnb", "flight", "travel"));
        KEYWORDS.put(SpendingCategory.INCOME, List.of(
                "salary", "payroll", "deposit", "refund", "reimbursement"));
        KEYWORDS.put(SpendingCategory.TRANSFER, List.of(
                "transfer", "send money", "peer transfer"));
    }

    @Override
    public List<ClassifiedTransaction> classify(List<TransactionInput> transactions) {
        List<ClassifiedTransaction> results = new ArrayList<>();
        if (transactions == null) {
            return results;
        }
        for (TransactionInput t : transactions) {
            if (t == null) {
                continue;
            }
            results.add(new ClassifiedTransaction(t.description(), t.amount(), categorize(t)));
        }
        return results;
    }

    private SpendingCategory categorize(TransactionInput t) {
        String d = t.description() == null ? "" : t.description().toLowerCase();
        for (Map.Entry<SpendingCategory, List<String>> entry : KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (d.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }
        if (isPositive(t.amount())) {
            return SpendingCategory.INCOME;
        }
        return SpendingCategory.OTHER;
    }

    private boolean isPositive(String amount) {
        if (amount == null || amount.isBlank()) {
            return false;
        }
        try {
            return new BigDecimal(amount).signum() > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
