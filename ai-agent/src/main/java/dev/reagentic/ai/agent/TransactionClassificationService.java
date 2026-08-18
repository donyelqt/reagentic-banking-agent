package dev.reagentic.ai.agent;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TransactionClassificationService {

    private final TransactionClassifier classifier;

    public TransactionClassificationService(TransactionClassifier classifier) {
        this.classifier = classifier;
    }

    public ClassifyResponse classify(List<TransactionInput> transactions) {
        List<ClassifiedTransaction> classified = classifier.classify(transactions);
        return new ClassifyResponse(classified, summarize(classified));
    }

    private List<CategoryTotal> summarize(List<ClassifiedTransaction> classified) {
        Map<SpendingCategory, BigDecimal> totals = new LinkedHashMap<>();
        Map<SpendingCategory, Integer> counts = new LinkedHashMap<>();
        for (ClassifiedTransaction t : classified) {
            BigDecimal amount = parseAmount(t.amount());
            totals.merge(t.category(), amount, BigDecimal::add);
            counts.merge(t.category(), 1, Integer::sum);
        }
        List<CategoryTotal> summary = new ArrayList<>();
        for (Map.Entry<SpendingCategory, BigDecimal> e : totals.entrySet()) {
            summary.add(new CategoryTotal(e.getKey(), e.getValue().toPlainString(), counts.get(e.getKey())));
        }
        return summary;
    }

    private BigDecimal parseAmount(String amount) {
        if (amount == null || amount.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(amount.replace(",", ""));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
