package dev.reagentic.notification.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "notifications")
public class NotificationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "type")
    private String type;

    @Column(name = "account_id")
    private String accountId;

    @Column(name = "amount")
    private String amount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at", nullable = false)
    private long createdAt = System.currentTimeMillis();

    protected NotificationRecord() {
    }

    public NotificationRecord(String paymentId, String type, String accountId, String amount,
                              String currency, String status) {
        this.paymentId = paymentId;
        this.type = type;
        this.accountId = accountId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getType() {
        return type;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getStatus() {
        return status;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
