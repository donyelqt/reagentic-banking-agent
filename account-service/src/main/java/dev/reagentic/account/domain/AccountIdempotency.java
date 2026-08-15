package dev.reagentic.account.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "account_idempotency",
        uniqueConstraints = @UniqueConstraint(name = "uk_acct_idem_key", columnNames = "idempotency_key"))
public class AccountIdempotency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String key;

    @Column(name = "account_id")
    private String accountId;

    @Column
    private String op;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    protected AccountIdempotency() {
    }

    public AccountIdempotency(String key, String accountId, String op, BigDecimal amount) {
        this.key = key;
        this.accountId = accountId;
        this.op = op;
        this.amount = amount;
    }

    public String getKey() {
        return key;
    }
}
