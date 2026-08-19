package dev.reagentic.ledger.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entry_id")
    private Long entryId;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(name = "payment_id")
    private String paymentId;

    @Column(nullable = false)
    private String type;

    @Column(name = "description")
    private String description;

    @Column(name = "signed_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal signedAmount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "created_at", nullable = false)
    private long createdAt = System.currentTimeMillis();

    protected LedgerEntry() {
    }

    public LedgerEntry(String accountId, String paymentId, String type, BigDecimal signedAmount, BigDecimal balanceAfter) {
        this.accountId = accountId;
        this.paymentId = paymentId;
        this.type = type;
        this.signedAmount = signedAmount;
        this.balanceAfter = balanceAfter;
    }

    public Long getEntryId() {
        return entryId;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getSignedAmount() {
        return signedAmount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
