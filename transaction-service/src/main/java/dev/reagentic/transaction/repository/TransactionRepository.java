package dev.reagentic.transaction.repository;

import dev.reagentic.transaction.model.Transaction;
import dev.reagentic.transaction.model.TransactionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findByAccountIdOrderByTransactionDateDesc(String accountId);

    List<Transaction> findByUploadBatchId(String uploadBatchId);

    void deleteByUploadBatchId(String uploadBatchId);

    List<Transaction> findByAccountIdAndCategory(String accountId, TransactionCategory category);

    List<Transaction> findByAccountIdAndTransactionDateBetween(
            String accountId, LocalDate startInclusive, LocalDate endInclusive);

    @Query("""
            SELECT t.category AS category, SUM(t.amount) AS total
            FROM Transaction t
            WHERE t.accountId = :accountId
            GROUP BY t.category
            """)
    List<CategoryTotal> sumAmountByCategoryForAccount(@Param("accountId") String accountId);

    interface CategoryTotal {
        TransactionCategory getCategory();
        BigDecimal getTotal();
    }
}
