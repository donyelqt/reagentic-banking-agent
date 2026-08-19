package dev.reagentic.ledger.service;

import dev.reagentic.ledger.domain.LedgerEntry;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class StatementCsvRenderer {

    private static final String BOM = "\uFEFF";
    private static final String HEADER = "date,description,type,reference,amount,balance";
    private static final String CRLF = "\r\n";

    private static final Map<String, String> DESCRIPTIONS = Map.of(
            "OPENING", "Opening balance",
            "DEBIT", "Transfer out",
            "CREDIT", "Transfer in",
            "DEBIT_FAILED", "Failed transfer",
            "COMPENSATE", "Refund");

    private StatementCsvRenderer() {
    }

    public static String render(List<LedgerEntry> entries) {
        StringBuilder sb = new StringBuilder(BOM).append(HEADER);
        for (LedgerEntry e : entries) {
            sb.append(CRLF)
                    .append(escape(Instant.ofEpochMilli(e.getCreatedAt()).toString()))
                    .append(',')
                    .append(escape(describe(e.getType())))
                    .append(',')
                    .append(escape(e.getType()))
                    .append(',')
                    .append(escape(e.getPaymentId()))
                    .append(',')
                    .append(escape(e.getSignedAmount().toPlainString()))
                    .append(',')
                    .append(escape(e.getBalanceAfter().toPlainString()));
        }
        sb.append(CRLF);
        return sb.toString();
    }

    private static String describe(String type) {
        return type == null ? "" : DESCRIPTIONS.getOrDefault(type, type);
    }

    private static String escape(String field) {
        if (field == null) {
            return "";
        }
        if (field.indexOf(',') < 0 && field.indexOf('"') < 0 && field.indexOf('\r') < 0 && field.indexOf('\n') < 0) {
            return field;
        }
        return '"' + field.replace("\"", "\"\"") + '"';
    }
}