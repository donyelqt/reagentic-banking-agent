package dev.reagentic.ai.agent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ClassifyRequestTest {

    private static TransactionInput tx(String description, String amount) {
        return new TransactionInput(description, amount);
    }

    @Test
    void validRequestPasses() {
        List<TransactionInput> items = List.of(
                tx("Starbucks Coffee", "-5.00"),
                tx("Payroll Deposit", "25000"),
                tx("Meralco Bill", "-1,234.56"),
                tx("Grab Ride", "-150.00"));

        assertNull(ClassifyRequest.validate(new ClassifyRequest(items)));
    }

    @Test
    void nullRequestRejected() {
        assertNotNull(ClassifyRequest.validate(null));
    }

    @Test
    void nullTransactionsRejected() {
        assertNotNull(ClassifyRequest.validate(new ClassifyRequest(null)));
    }

    @Test
    void emptyListIsAccepted() {
        assertNull(ClassifyRequest.validate(new ClassifyRequest(List.of())));
    }

    @Test
    void overMaxItemsRejected() {
        List<TransactionInput> items = new ArrayList<>();
        for (int i = 0; i <= ClassifyRequest.MAX_ITEMS; i++) {
            items.add(tx("item " + i, "-1.00"));
        }
        assertNotNull(ClassifyRequest.validate(new ClassifyRequest(items)));
    }

    @Test
    void exactlyMaxItemsAccepted() {
        List<TransactionInput> items = new ArrayList<>();
        for (int i = 0; i < ClassifyRequest.MAX_ITEMS; i++) {
            items.add(tx("item " + i, "-1.00"));
        }
        assertNull(ClassifyRequest.validate(new ClassifyRequest(items)));
    }

    @Test
    void nullElementRejected() {
        List<TransactionInput> items = new ArrayList<>();
        items.add(tx("Starbucks Coffee", "-5.00"));
        items.add(null);
        assertNotNull(ClassifyRequest.validate(new ClassifyRequest(items)));
    }

    @Test
    void blankDescriptionRejected() {
        assertNotNull(ClassifyRequest.validate(new ClassifyRequest(List.of(tx("   ", "-5.00")))));
    }

    @Test
    void descriptionOverLengthRejected() {
        String longDescription = "x".repeat(ClassifyRequest.MAX_DESCRIPTION_LENGTH + 1);
        assertNotNull(ClassifyRequest.validate(new ClassifyRequest(List.of(tx(longDescription, "-5.00")))));
    }

    @Test
    void garbageAmountRejected() {
        assertNotNull(ClassifyRequest.validate(new ClassifyRequest(List.of(tx("Starbucks Coffee", "abc")))));
    }

    @Test
    void threeDecimalPlacesRejected() {
        assertNotNull(ClassifyRequest.validate(new ClassifyRequest(List.of(tx("Starbucks Coffee", "1.999")))));
    }

    @Test
    void rejectsWholeNumberAmountWithoutDecimalsIssue() {
        assertNull(ClassifyRequest.validate(new ClassifyRequest(List.of(tx("Starbucks Coffee", "-5")))));
    }

    @Test
    void errorNamesTheOffendingIndex() {
        List<TransactionInput> items = List.of(
                tx("Starbucks Coffee", "-5.00"),
                tx("Bad", "not-a-number"));
        String error = ClassifyRequest.validate(new ClassifyRequest(items));

        assertNotNull(error);
        assertEquals("transactions[1].amount 'not-a-number' is not a valid amount", error);
    }

    @Test
    void summarizerParsesValidatedAmounts() {
        TransactionClassificationService service = new TransactionClassificationService(
                new KeywordTransactionClassifier());
        ClassifyResponse response = service.classify(List.of(tx("SM Supermarket", "1,000.00")));

        assertEquals("1000.00", response.summary().get(0).total());
        assertEquals(SpendingCategory.GROCERIES, response.summary().get(0).category());
    }
}