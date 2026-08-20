package dev.reagentic.ledger.service;

import dev.reagentic.common.events.PaymentCompletedEvent;
import dev.reagentic.common.events.PaymentFailedEvent;
import dev.reagentic.common.money.Money;
import dev.reagentic.ledger.domain.LedgerEntry;
import dev.reagentic.ledger.repository.LedgerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LedgerServiceTest {

    private static final String CHECKING = "acc-checking-0001";
    private static final String SAVINGS = "acc-savings-0002";

    private final LedgerRepository repository = mock(LedgerRepository.class);
    private final LedgerService service = new LedgerService(repository);

    private static LedgerEntry entry(String paymentId, String type, String signed, String balanceAfter) {
        return new LedgerEntry(CHECKING, paymentId, type,
                new BigDecimal(signed), new BigDecimal(balanceAfter));
    }

    private static PaymentCompletedEvent completed(String paymentId) {
        return new PaymentCompletedEvent(paymentId, CHECKING, SAVINGS,
                Money.of("50.00"), "USD", paymentId, 1L);
    }

    private static PaymentFailedEvent failed(String paymentId, boolean debitApplied, boolean compensateApplied) {
        return new PaymentFailedEvent(paymentId, CHECKING, Money.of("50.00"), "USD",
                "credit failed", paymentId, debitApplied, compensateApplied, 1L);
    }

    @Test
    void applyCompletedAppendsDebitAndCreditLegsChainingFromLastBalance() {
        when(repository.existsByPaymentId("pmt-1")).thenReturn(false);
        when(repository.findTopByAccountIdOrderByEntryIdDescForUpdate(CHECKING))
                .thenReturn(Optional.of(entry(null, "OPENING", "0.00", "1000.00")));
        when(repository.findTopByAccountIdOrderByEntryIdDescForUpdate(SAVINGS))
                .thenReturn(Optional.of(entry(null, "OPENING", "0.00", "500.00")));

        service.applyCompleted(completed("pmt-1"));

        verify(repository, times(2)).save(any());
        verify(repository).save(argThatEntry("pmt-1", "DEBIT", "-50.00", "950.00"));
        verify(repository).save(argThatEntry("pmt-1", "CREDIT", "50.00", "550.00"));
    }

    @Test
    void applyFailedWithCompensationAppendsDebitFailedAndCompensateLegsNettingZero() {
        when(repository.existsByPaymentId("pmt-2")).thenReturn(false);
        when(repository.findTopByAccountIdOrderByEntryIdDescForUpdate(CHECKING))
                .thenReturn(Optional.of(entry(null, "OPENING", "0.00", "1000.00")))
                .thenReturn(Optional.of(entry("pmt-2", "DEBIT_FAILED", "-50.00", "950.00")));

        service.applyFailed(failed("pmt-2", true, true));

        verify(repository, times(2)).save(any());
        verify(repository).save(argThatEntry("pmt-2", "DEBIT_FAILED", "-50.00", "950.00"));
        verify(repository).save(argThatEntry("pmt-2", "COMPENSATE", "50.00", "1000.00"));
    }

    @Test
    void applyFailedWithoutCompensationAppendsOnlyDebitLegReflectingTheRemainingDebit() {
        when(repository.existsByPaymentId("pmt-3")).thenReturn(false);
        when(repository.findTopByAccountIdOrderByEntryIdDescForUpdate(CHECKING))
                .thenReturn(Optional.of(entry(null, "OPENING", "0.00", "1000.00")));

        service.applyFailed(failed("pmt-3", true, false));

        verify(repository, times(1)).save(any());
        verify(repository).save(argThatEntry("pmt-3", "DEBIT_FAILED", "-50.00", "950.00"));
    }

    @Test
    void applyFailedBeforeDebitRecordsNothing() {
        when(repository.existsByPaymentId("pmt-4")).thenReturn(false);

        service.applyFailed(failed("pmt-4", false, false));

        verify(repository, never()).save(any());
    }

    @Test
    void duplicateDeliveryOfCompletedPaymentIsIgnored() {
        when(repository.existsByPaymentId("pmt-5")).thenReturn(true);

        service.applyCompleted(completed("pmt-5"));

        verify(repository, never()).save(any());
    }

    @Test
    void duplicateDeliveryOfFailedPaymentIsIgnored() {
        when(repository.existsByPaymentId("pmt-6")).thenReturn(true);

        service.applyFailed(failed("pmt-6", true, true));

        verify(repository, never()).save(any());
    }

    @Test
    void uniqueConstraintViolationOnRacingRedeliveryIsTreatedAsIdempotent() {
        when(repository.existsByPaymentId("pmt-7")).thenReturn(false);
        when(repository.findTopByAccountIdOrderByEntryIdDescForUpdate(CHECKING))
                .thenReturn(Optional.of(entry(null, "OPENING", "0.00", "1000.00")));
        when(repository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        assertDoesNotThrow(() -> service.applyCompleted(completed("pmt-7")));

        verify(repository, times(1)).save(any());
    }

    private static LedgerEntry argThatEntry(String paymentId, String type, String signed, String balanceAfter) {
        return org.mockito.ArgumentMatchers.argThat(e ->
                e.getPaymentId().equals(paymentId)
                        && e.getType().equals(type)
                        && e.getSignedAmount().compareTo(new BigDecimal(signed)) == 0
                        && e.getBalanceAfter().compareTo(new BigDecimal(balanceAfter)) == 0);
    }
}