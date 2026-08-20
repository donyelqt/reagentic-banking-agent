package dev.reagentic.common.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.reagentic.common.money.Money;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentFailedEventTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void legacyEventWithoutCompensateAppliedDefaultsToDebitApplied() throws Exception {
        String modern = mapper.writeValueAsString(new PaymentFailedEvent(
                "pmt-1", "acc-checking-0001", Money.of("50.00"), "USD",
                "credit failed", "k-1", true, 1L));
        String legacy = modern.replace(",\"compensateApplied\":true", "");

        PaymentFailedEvent parsed = mapper.readValue(legacy, PaymentFailedEvent.class);

        assertTrue(parsed.debitApplied());
        assertTrue(parsed.compensateApplied());
    }

    @Test
    void modernEventPreservesDistinctFlags() throws Exception {
        String json = mapper.writeValueAsString(new PaymentFailedEvent(
                "pmt-2", "acc-checking-0001", Money.of("50.00"), "USD",
                "credit failed", "k-2", true, false, 1L));

        PaymentFailedEvent parsed = mapper.readValue(json, PaymentFailedEvent.class);

        assertTrue(parsed.debitApplied());
        assertFalse(parsed.compensateApplied());
    }
}