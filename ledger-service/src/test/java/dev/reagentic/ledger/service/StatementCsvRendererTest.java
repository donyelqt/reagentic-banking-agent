package dev.reagentic.ledger.service;

import dev.reagentic.ledger.domain.LedgerEntry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatementCsvRendererTest {

    private static LedgerEntry entry(long createdAt, String paymentId, String type, String signed, String balanceAfter) {
        LedgerEntry e = new LedgerEntry("acc-checking-0001", paymentId, type,
                new BigDecimal(signed), new BigDecimal(balanceAfter));
        try {
            Field f = LedgerEntry.class.getDeclaredField("createdAt");
            f.setAccessible(true);
            f.setLong(e, createdAt);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        return e;
    }

    private static String[] lines(String csv) {
        String[] parts = csv.substring(1).split("\r\n");
        return parts[parts.length - 1].isEmpty() ? Arrays.copyOf(parts, parts.length - 1) : parts;
    }

    @Test
    void startsWithUtf8Bom() {
        assertTrue(StatementCsvRenderer.render(List.of()).startsWith("\uFEFF"));
    }

    @Test
    void rendersHeaderRowFirst() {
        String[] lines = lines(StatementCsvRenderer.render(List.of()));
        assertEquals("date,type,payment_id,signed_amount,balance_after", lines[0]);
    }

    @Test
    void rendersOneRowPerEntry() {
        List<LedgerEntry> entries = List.of(
                entry(1785324900000L, "OPENING", "OPENING", "1000.00", "1000.00"),
                entry(1785411300000L, "pmt-1", "DEBIT", "-50.00", "950.00"),
                entry(1785497700000L, "pmt-2", "CREDIT", "25.00", "975.00"));
        assertEquals(1 + entries.size(), lines(StatementCsvRenderer.render(entries)).length);
    }

    @Test
    void usesCrlfLineEndings() {
        String csv = StatementCsvRenderer.render(List.of(entry(1785324900000L, "pmt-1", "DEBIT", "-50.00", "950.00")));
        assertTrue(csv.contains("\r\n"));
        assertTrue(csv.endsWith("\r\n"));
        assertEquals(-1, csv.replace("\r\n", "").indexOf('\n'));
    }

    @Test
    void formatsCreatedAtAsIsoUtc() {
        String[] lines = lines(StatementCsvRenderer.render(
                List.of(entry(1785324900000L, "pmt-1", "DEBIT", "-50.00", "950.00"))));
        assertTrue(lines[1].startsWith("2026-07-29T11:35:00Z,"));
    }

    @Test
    void formatsSignedAmountsPlainAndSigned() {
        String[] lines = lines(StatementCsvRenderer.render(List.of(
                entry(1785324900000L, "pmt-1", "DEBIT", "-50.00", "950.00"),
                entry(1785411300000L, "pmt-2", "CREDIT", "25.00", "975.00"))));
        assertTrue(lines[1].contains(",-50.00,"));
        assertTrue(lines[2].contains(",25.00,"));
    }

    @Test
    void balanceColumnMatchesEntryBalanceAfter() {
        String[] lines = lines(StatementCsvRenderer.render(List.of(
                entry(1785324900000L, "pmt-1", "DEBIT", "-50.00", "950.00"),
                entry(1785411300000L, "pmt-2", "CREDIT", "25.00", "975.00"))));
        assertEquals("950.00", lines[1].split(",")[4]);
        assertEquals("975.00", lines[2].split(",")[4]);
    }

    @Test
    void quotesFieldsContainingCommaOrQuote() {
        String[] lines = lines(StatementCsvRenderer.render(
                List.of(entry(1785324900000L, "pmt,1\"x", "DEBIT", "-50.00", "950.00"))));
        assertTrue(lines[1].contains("\"pmt,1\"\"x\""));
    }

    @Test
    void rendersEmptyLedgerAsHeaderOnly() {
        assertEquals(1, lines(StatementCsvRenderer.render(List.of())).length);
    }
}