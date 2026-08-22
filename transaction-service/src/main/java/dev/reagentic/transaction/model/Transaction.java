package dev.reagentic.transaction.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transactions_account_id", columnList = "accountId"),
        @Index(name = "idx_transactions_transaction_date", columnList = "transactionDate")
})
public class Transaction {

    @Id
    private String id;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private LocalDate transactionDate;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(length = 255)
    private String rawCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionCategory category;

    @Column(nullable = false)
    private String uploadBatchId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Transaction() {
        this.id = UUID.randomUUID().toString();
        this.category = TransactionCategory.UNCATEGORIZED;
        this.createdAt = Instant.now();
    }

    public Transaction(String id, String accountId, LocalDate transactionDate, String description,
                       BigDecimal amount, String rawCategory, TransactionCategory category,
                       String uploadBatchId, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.accountId = accountId;
        this.transactionDate = transactionDate;
        this.description = description;
        this.amount = amount;
        this.rawCategory = rawCategory;
        this.category = category != null ? category : TransactionCategory.UNCATEGORIZED;
        this.uploadBatchId = uploadBatchId;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getRawCategory() {
        return rawCategory;
    }

    public void setRawCategory(String rawCategory) {
        this.rawCategory = rawCategory;
    }

    public TransactionCategory getCategory() {
        return category;
    }

    public void setCategory(TransactionCategory category) {
        this.category = category;
    }

    public String getUploadBatchId() {
        return uploadBatchId;
    }

    public void setUploadBatchId(String uploadBatchId) {
        this.uploadBatchId = uploadBatchId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public static TransactionBuilder builder() {
        return new TransactionBuilder();
    }

    public static class TransactionBuilder {
        private String id;
        private String accountId;
        private LocalDate transactionDate;
        private String description;
        private BigDecimal amount;
        private String rawCategory;
        private TransactionCategory category;
        private String uploadBatchId;
        private Instant createdAt;

        public TransactionBuilder id(String id) {
            this.id = id;
            return this;
        }

        public TransactionBuilder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public TransactionBuilder transactionDate(LocalDate transactionDate) {
            this.transactionDate = transactionDate;
            return this;
        }

        public TransactionBuilder description(String description) {
            this.description = description;
            return this;
        }

        public TransactionBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public TransactionBuilder rawCategory(String rawCategory) {
            this.rawCategory = rawCategory;
            return this;
        }

        public TransactionBuilder category(TransactionCategory category) {
            this.category = category;
            return this;
        }

        public TransactionBuilder uploadBatchId(String uploadBatchId) {
            this.uploadBatchId = uploadBatchId;
            return this;
        }

        public TransactionBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Transaction build() {
            return new Transaction(id, accountId, transactionDate, description, amount, rawCategory, category, uploadBatchId, createdAt);
        }
    }
}
