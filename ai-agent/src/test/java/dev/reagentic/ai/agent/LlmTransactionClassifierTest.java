package dev.reagentic.ai.agent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
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

    @Test
    void exactOneToOneResponseIsTrusted() {
        ChatClient client = clientReturning(new ClassificationDto(List.of(
                new ClassificationItemDto("Starbucks Coffee", "DINING"),
                new ClassificationItemDto("Payroll Deposit", "INCOME"))));
        ChatClientProvider provider = mock(ChatClientProvider.class);
        when(provider.client()).thenReturn(client);
        LlmTransactionClassifier classifier = build(provider);
        List<TransactionInput> inputs = List.of(
                new TransactionInput("Starbucks Coffee", "-5.00"),
                new TransactionInput("Payroll Deposit", "25000.00"));

        List<ClassifiedTransaction> result = classifier.classify(inputs);

        assertEquals(2, result.size());
        assertEquals(SpendingCategory.DINING, result.get(0).category());
        assertEquals(SpendingCategory.INCOME, result.get(1).category());
        assertEquals("Starbucks Coffee", result.get(0).description());
        assertEquals("-5.00", result.get(0).amount());
    }

    @Test
    void unknownCategoryFallsBackOnlyThatItem() {
        ChatClient client = clientReturning(new ClassificationDto(List.of(
                new ClassificationItemDto("Starbucks Coffee", "NOT_A_CATEGORY"),
                new ClassificationItemDto("Netflix Monthly", "SUBSCRIPTIONS"))));
        ChatClientProvider provider = mock(ChatClientProvider.class);
        when(provider.client()).thenReturn(client);
        LlmTransactionClassifier classifier = build(provider);
        List<TransactionInput> inputs = List.of(
                new TransactionInput("Starbucks Coffee", "-5.00"),
                new TransactionInput("Netflix Monthly", "-15.99"));

        List<ClassifiedTransaction> result = classifier.classify(inputs);

        assertEquals(SpendingCategory.DINING, result.get(0).category());
        assertEquals(SpendingCategory.SUBSCRIPTIONS, result.get(1).category());
    }

    @Test
    void reorderedResponseNeverSilentlyMislabels() {
        ChatClient client = clientReturning(new ClassificationDto(List.of(
                new ClassificationItemDto("Netflix Monthly", "SUBSCRIPTIONS"),
                new ClassificationItemDto("Starbucks Coffee", "DINING"))));
        ChatClientProvider provider = mock(ChatClientProvider.class);
        when(provider.client()).thenReturn(client);
        LlmTransactionClassifier classifier = build(provider);
        List<TransactionInput> inputs = List.of(
                new TransactionInput("Starbucks Coffee", "-5.00"),
                new TransactionInput("Netflix Monthly", "-15.99"));

        List<ClassifiedTransaction> result = classifier.classify(inputs);

        assertEquals(KEYWORD.classify(inputs), result);
    }

    @Test
    void countMismatchFallsBackWholeBatch() {
        ChatClient client = clientReturning(new ClassificationDto(List.of(
                new ClassificationItemDto("Starbucks Coffee", "DINING"))));
        ChatClientProvider provider = mock(ChatClientProvider.class);
        when(provider.client()).thenReturn(client);
        LlmTransactionClassifier classifier = build(provider);
        List<TransactionInput> inputs = List.of(
                new TransactionInput("Starbucks Coffee", "-5.00"),
                new TransactionInput("Netflix Monthly", "-15.99"));

        List<ClassifiedTransaction> result = classifier.classify(inputs);

        assertEquals(KEYWORD.classify(inputs), result);
    }

    @Test
    void llmFailureFallsBackWholeBatch() {
        ChatClient client = mock(ChatClient.class);
        when(client.prompt()).thenThrow(new RuntimeException("model unreachable"));
        ChatClientProvider provider = mock(ChatClientProvider.class);
        when(provider.client()).thenReturn(client);
        LlmTransactionClassifier classifier = build(provider);
        List<TransactionInput> inputs = List.of(new TransactionInput("SM Supermarket", "-1200.50"));

        List<ClassifiedTransaction> result = classifier.classify(inputs);

        assertEquals(KEYWORD.classify(inputs), result);
    }

    private ChatClient clientReturning(ClassificationDto dto) {
        ChatClient client = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec call = mock(ChatClient.CallResponseSpec.class);
        when(client.prompt()).thenReturn(request);
        when(request.system(anyString())).thenReturn(request);
        when(request.user(anyString())).thenReturn(request);
        when(request.call()).thenReturn(call);
        when(call.entity(ClassificationDto.class)).thenReturn(dto);
        return client;
    }
}