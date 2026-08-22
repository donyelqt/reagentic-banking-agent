package dev.reagentic.transaction.service;

import dev.reagentic.transaction.model.Transaction;
import dev.reagentic.transaction.model.TransactionCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionCategorizationServiceTest {

    private final TransactionCategorizationService service = new TransactionCategorizationService();

    @Test
    void categorizesDiningKeyword() {
        Transaction t = Transaction.builder()
                .description("Jollibee BGC Branch")
                .amount(new BigDecimal("-150.00"))
                .transactionDate(LocalDate.now())
                .uploadBatchId("b1")
                .accountId("acc-1")
                .build();

        service.categorize(t);
        assertEquals(TransactionCategory.DINING, t.getCategory());
    }

    @Test
    void categorizesGroceriesKeyword() {
        Transaction t = Transaction.builder()
                .description("SM Supermarket Grocery")
                .amount(new BigDecimal("-1250.00"))
                .transactionDate(LocalDate.now())
                .uploadBatchId("b1")
                .accountId("acc-1")
                .build();

        service.categorize(t);
        assertEquals(TransactionCategory.GROCERIES, t.getCategory());
    }

    @Test
    void categorizesPositiveAmountAsIncome() {
        Transaction t = Transaction.builder()
                .description("Payroll Deposit")
                .amount(new BigDecimal("5000.00"))
                .transactionDate(LocalDate.now())
                .uploadBatchId("b1")
                .accountId("acc-1")
                .build();

        service.categorize(t);
        assertEquals(TransactionCategory.INCOME, t.getCategory());
    }
}
