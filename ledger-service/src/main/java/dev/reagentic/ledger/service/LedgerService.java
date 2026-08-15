package dev.reagentic.ledger.service;

import dev.reagentic.common.events.PaymentCompletedEvent;
import dev.reagentic.common.events.PaymentFailedEvent;
import dev.reagentic.common.money.Money;
import dev.reagentic.ledger.domain.LedgerEntry;
import dev.reagentic.ledger.repository.LedgerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class LedgerService {

    private static final Logger log = LoggerFactory.getLogger(LedgerService.class);

    private final LedgerRepository repository;

    @Value("${ledger.fault.skip-append:false}")
    private boolean skipAppend;

    public LedgerService(LedgerRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void applyCompleted(PaymentCompletedEvent e) {
        if (repository.existsByPaymentId(e.paymentId())) {
            return;
        }
        if (skipAppend) {
            log.warn("LEDGER FAULT INJECTED: skipping append for payment {}", e.paymentId());
            return;
        }
        append(e.sourceAccountId(), e.paymentId(), "DEBIT", e.amount().negate());
        append(e.destinationAccountId(), e.paymentId(), "CREDIT", e.amount());
    }

    @Transactional
    public void applyFailed(PaymentFailedEvent e) {
        if (repository.existsByPaymentId(e.paymentId())) {
            return;
        }
        if (skipAppend) {
            log.warn("LEDGER FAULT INJECTED: skipping append for payment {}", e.paymentId());
            return;
        }
        append(e.sourceAccountId(), e.paymentId(), "DEBIT_FAILED", e.amount().negate());
        append(e.sourceAccountId(), e.paymentId(), "COMPENSATE", e.amount());
    }

    private void append(String accountId, String paymentId, String type, Money signed) {
        LedgerEntry last = repository.findTopByAccountIdOrderByCreatedAtDesc(accountId).orElse(null);
        BigDecimal prev = last == null ? BigDecimal.ZERO : last.getBalanceAfter();
        BigDecimal balanceAfter = prev.add(signed.value());
        repository.save(new LedgerEntry(accountId, paymentId, type, signed.value(), balanceAfter));
    }
}
