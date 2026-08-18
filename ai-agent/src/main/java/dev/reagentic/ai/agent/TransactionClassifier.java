package dev.reagentic.ai.agent;

import java.util.List;

public interface TransactionClassifier {
    List<ClassifiedTransaction> classify(List<TransactionInput> transactions);
}
