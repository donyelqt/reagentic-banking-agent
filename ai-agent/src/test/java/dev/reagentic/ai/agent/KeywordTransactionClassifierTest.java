package dev.reagentic.ai.agent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeywordTransactionClassifierTest {

    private final KeywordTransactionClassifier classifier = new KeywordTransactionClassifier();

    private SpendingCategory categoryOf(String description, String amount) {
        List<ClassifiedTransaction> result = classifier.classify(
                List.of(new TransactionInput(description, amount)));
        return result.get(0).category();
    }

    @Test
    void diningKeywordsClassifyAsDining() {
        assertEquals(SpendingCategory.DINING, categoryOf("Starbucks Coffee", "-5.50"));
        assertEquals(SpendingCategory.DINING, categoryOf("Jollibee Delivery", "-8.00"));
    }

    @Test
    void groceryKeywordsClassifyAsGroceries() {
        assertEquals(SpendingCategory.GROCERIES, categoryOf("SM Supermarket", "-45.00"));
    }

    @Test
    void subscriptionKeywordsClassifyAsSubscriptions() {
        assertEquals(SpendingCategory.SUBSCRIPTIONS, categoryOf("Netflix Monthly", "-15.99"));
    }

    @Test
    void transportKeywordsClassifyAsTransport() {
        assertEquals(SpendingCategory.TRANSPORT, categoryOf("Grab Ride Home", "-7.25"));
    }

    @Test
    void utilityKeywordsClassifyAsUtilities() {
        assertEquals(SpendingCategory.UTILITIES, categoryOf("Meralco Bill Payment", "-60.00"));
        assertEquals(SpendingCategory.UTILITIES, categoryOf("Crestview Apartments Rent", "-1200.00"));
    }

    @Test
    void healthKeywordsClassifyAsHealth() {
        assertEquals(SpendingCategory.HEALTH, categoryOf("Gym & Fitness", "-55.00"));
    }

    @Test
    void positiveAmountWithoutExpenseKeywordClassifiesAsIncome() {
        assertEquals(SpendingCategory.INCOME, categoryOf("Payroll Deposit", "1500.00"));
    }

    @Test
    void unrecognizedNegativeDescriptionClassifiesAsOther() {
        assertEquals(SpendingCategory.OTHER, categoryOf("Miscellaneous XYZ123", "-3.00"));
    }

    @Test
    void classifyPreservesOrderAndCount() {
        List<TransactionInput> inputs = List.of(
                new TransactionInput("Starbucks Coffee", "-5.00"),
                new TransactionInput("Netflix Monthly", "-15.99"),
                new TransactionInput("Payroll Deposit", "1500.00"));

        List<ClassifiedTransaction> result = classifier.classify(inputs);

        assertEquals(3, result.size());
        assertEquals("Starbucks Coffee", result.get(0).description());
        assertEquals(SpendingCategory.DINING, result.get(0).category());
        assertEquals(SpendingCategory.SUBSCRIPTIONS, result.get(1).category());
        assertEquals(SpendingCategory.INCOME, result.get(2).category());
    }
}
