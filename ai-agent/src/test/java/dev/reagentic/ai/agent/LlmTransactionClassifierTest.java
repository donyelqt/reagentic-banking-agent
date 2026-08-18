package dev.reagentic.ai.agent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmTransactionClassifierTest {

    private static final KeywordTransactionClassifier KEYWORD = new KeywordTransactionClassifier();

    private LlmTransactionClassifier build(ChatClientProvider provider) {
        return new LlmTransactionClassifier(KEYWORD, provider);
    }

    @Test
    void emptyInputReturnsEmptyList() {
        ChatClientProvider provider = mock(ChatClientProvider.class);
        LlmTransactionClassifier classifier = build(provider);

        assertTrue(classifier.classify(List.of()).isEmpty());
    }

    @Test
    void noClientConfiguredFallsBackToKeywordClassifier() {
        ChatClientProvider provider = mock(ChatClientProvider.class);
        when(provider.client()).thenReturn(null);
        LlmTransactionClassifier classifier = build(provider);
        List<TransactionInput> inputs = List.of(new TransactionInput("Starbucks Coffee", "-5.00"));

        List<ClassifiedTransaction> result = classifier.classify(inputs);

        assertEquals(KEYWORD.classify(inputs), result);
    }
}
