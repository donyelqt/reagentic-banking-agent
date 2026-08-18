package dev.reagentic.ai.agent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransactionClassificationServiceTest {

    @Test
    void summarizesTotalsAndCountsPerCategory() {
        TransactionClassifier classifier = mock(TransactionClassifier.class);
        List<TransactionInput> inputs = List.of(
                new TransactionInput("Starbucks Coffee", "-5.00"),
                new TransactionInput("Jollibee", "-8.00"),
                new TransactionInput("Netflix Monthly", "-15.99"),
                new TransactionInput("Payroll Deposit", "1500.00"));
        List<ClassifiedTransaction> classified = List.of(
                new ClassifiedTransaction("Starbucks Coffee", "-5.00", SpendingCategory.DINING),
                new ClassifiedTransaction("Jollibee", "-8.00", SpendingCategory.DINING),
                new ClassifiedTransaction("Netflix Monthly", "-15.99", SpendingCategory.SUBSCRIPTIONS),
                new ClassifiedTransaction("Payroll Deposit", "1500.00", SpendingCategory.INCOME));
        when(classifier.classify(inputs)).thenReturn(classified);
        TransactionClassificationService service = new TransactionClassificationService(classifier);

        ClassifyResponse resp = service.classify(inputs);

        assertEquals(classified, resp.transactions());
        CategoryTotal dining = resp.summary().stream()
                .filter(c -> c.category() == SpendingCategory.DINING).findFirst().orElseThrow();
        assertEquals("-13.00", dining.total());
        assertEquals(2, dining.count());
        CategoryTotal income = resp.summary().stream()
                .filter(c -> c.category() == SpendingCategory.INCOME).findFirst().orElseThrow();
        assertEquals("1500.00", income.total());
        assertEquals(1, income.count());
        assertTrue(resp.summary().stream().anyMatch(c -> c.category() == SpendingCategory.SUBSCRIPTIONS));
    }

    @Test
    void emptyTransactionsProduceEmptySummary() {
        TransactionClassifier classifier = mock(TransactionClassifier.class);
        when(classifier.classify(List.of())).thenReturn(List.of());
        TransactionClassificationService service = new TransactionClassificationService(classifier);

        ClassifyResponse resp = service.classify(List.of());

        assertTrue(resp.transactions().isEmpty());
        assertTrue(resp.summary().isEmpty());
    }
}
