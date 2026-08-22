package dev.reagentic.transaction.service;

import dev.reagentic.transaction.model.Transaction;
import dev.reagentic.transaction.model.TransactionCategory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TransactionCategorizationService {

    private static final Map<TransactionCategory, List<String>> KEYWORD_RULES = Map.ofEntries(
            Map.entry(TransactionCategory.DINING, List.of("jollibee", "mcdo", "mcdonalds", "starbucks", "restaurant", "cafe", "food", "grab food", "kfc", "bento")),
            Map.entry(TransactionCategory.GROCERIES, List.of("supermarket", "grocery", "sm market", "puregold", "robinsons", "walmart", "target")),
            Map.entry(TransactionCategory.TRANSPORT, List.of("grab", "uber", "taxi", "gas", "fuel", "shell", "petron", "caltex", "toll", "mrt", "lrt")),
            Map.entry(TransactionCategory.SUBSCRIPTIONS, List.of("netflix", "spotify", "apple", "google", "subscription", "prime", "disney+")),
            Map.entry(TransactionCategory.UTILITIES, List.of("meralco", "maynilad", "manila water", "pldt", "globe", "smart", "electric", "water bill", "internet")),
            Map.entry(TransactionCategory.RENT, List.of("rent", "lease", "housing", "condo dues")),
            Map.entry(TransactionCategory.ENTERTAINMENT, List.of("cinema", "movie", "concert", "game", "steam", "playstation", "xbox")),
            Map.entry(TransactionCategory.SHOPPING, List.of("shopee", "lazada", "amazon", "mall", "zara", "uniqlo", "shopping")),
            Map.entry(TransactionCategory.HEALTH, List.of("pharmacy", "clinic", "hospital", "mercury drug", "watsons", "doctor")),
            Map.entry(TransactionCategory.TRANSFER, List.of("transfer", "send money", "wire", "remittance"))
    );

    public void categorize(Transaction transaction) {
        String haystack = (transaction.getDescription() + " " +
                (transaction.getRawCategory() != null ? transaction.getRawCategory() : ""))
                .toLowerCase(Locale.ROOT);

        for (var entry : KEYWORD_RULES.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (haystack.contains(keyword)) {
                    transaction.setCategory(entry.getKey());
                    return;
                }
            }
        }

        if (matchesRawCategoryEnum(transaction)) {
            return;
        }

        if (transaction.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            transaction.setCategory(TransactionCategory.INCOME);
        } else {
            transaction.setCategory(TransactionCategory.UNCATEGORIZED);
        }
    }

    public void categorizeAll(List<Transaction> transactions) {
        transactions.forEach(this::categorize);
    }

    private boolean matchesRawCategoryEnum(Transaction transaction) {
        if (transaction.getRawCategory() == null) {
            return false;
        }
        try {
            TransactionCategory match = TransactionCategory.valueOf(
                    transaction.getRawCategory().trim().toUpperCase(Locale.ROOT));
            transaction.setCategory(match);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
