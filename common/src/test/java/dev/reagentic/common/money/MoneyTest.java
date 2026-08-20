package dev.reagentic.common.money;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTest {

    @Test
    void acceptsPlainDecimals() {
        assertEquals("0.00", Money.of("0").asString());
        assertEquals("0.00", Money.of("0.00").asString());
        assertEquals("100.00", Money.of("100").asString());
        assertEquals("100.50", Money.of("100.5").asString());
        assertEquals("100.55", Money.of("100.55").asString());
        assertEquals("0.05", Money.of("0.05").asString());
    }

    @Test
    void rejectsScientificNotation() {
        assertThrows(IllegalArgumentException.class, () -> Money.of("1e2"));
        assertThrows(IllegalArgumentException.class, () -> Money.of("1E2"));
    }

    @Test
    void rejectsGarbageAndNonPlainGrammar() {
        assertThrows(IllegalArgumentException.class, () -> Money.of("abc"));
        assertThrows(IllegalArgumentException.class, () -> Money.of("1.234"));
        assertThrows(IllegalArgumentException.class, () -> Money.of("12.345"));
        assertThrows(IllegalArgumentException.class, () -> Money.of("-5"));
        assertThrows(IllegalArgumentException.class, () -> Money.of("+5"));
        assertThrows(IllegalArgumentException.class, () -> Money.of(".5"));
        assertThrows(IllegalArgumentException.class, () -> Money.of("5."));
        assertThrows(IllegalArgumentException.class, () -> Money.of("1,000"));
        assertThrows(IllegalArgumentException.class, () -> Money.of("1 000"));
    }

    @Test
    void rejectsBlankAndNull() {
        assertThrows(IllegalArgumentException.class, () -> Money.of((String) null));
        assertThrows(IllegalArgumentException.class, () -> Money.of(""));
        assertThrows(IllegalArgumentException.class, () -> Money.of("   "));
    }

    @Test
    void ofBigDecimalNormalizesScientificInputToPlainString() {
        // Internal callers pass BigDecimals; of(BigDecimal) goes through
        // toPlainString so scientific notation is legal there and normalizes to
        // the strict plain grammar enforced on the wire.
        assertEquals("100.00", Money.of(new BigDecimal("1e2")).asString());
        assertEquals("0.50", Money.of(new BigDecimal("5e-1")).asString());
    }
}