package dev.reagentic.transaction.util;

import dev.reagentic.transaction.model.Transaction;
import org.apache.commons.csv.CSVRecord;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class CsvRowMapper {

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,          // 2026-08-01
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),  // 08/01/2026
            DateTimeFormatter.ofPattern("dd/MM/yyyy")   // 01/08/2026
    );

    private CsvRowMapper() {
    }

    public static Transaction map(CSVRecord record, String accountId, String uploadBatchId) {
        String rawDate = safeGet(record, "date");
        String description = safeGet(record, "description");
        String rawAmount = safeGet(record, "amount");
        String rawCategory = safeGet(record, "category");

        if (rawDate.isBlank() || description.isBlank() || rawAmount.isBlank()) {
            throw new CsvRowException(record.getRecordNumber(),
                    "Missing required field(s) — date, description, and amount are required");
        }

        LocalDate date = parseDate(record.getRecordNumber(), rawDate);
        BigDecimal amount = parseAmount(record.getRecordNumber(), rawAmount);

        return Transaction.builder()
                .accountId(accountId)
                .transactionDate(date)
                .description(description.trim())
                .amount(amount)
                .rawCategory(rawCategory.isBlank() ? null : rawCategory.trim())
                .uploadBatchId(uploadBatchId)
                .build();
    }

    private static String safeGet(CSVRecord record, String column) {
        try {
            return record.isMapped(column) ? record.get(column).trim() : "";
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    private static LocalDate parseDate(long rowNumber, String raw) {
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(raw, fmt);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new CsvRowException(rowNumber, "Unrecognized date format: '" + raw + "'");
    }

    private static BigDecimal parseAmount(long rowNumber, String raw) {
        try {
            String cleaned = raw.replaceAll("[₱$,]", "").trim();
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            throw new CsvRowException(rowNumber, "Unparseable amount: '" + raw + "'");
        }
    }

    public static class CsvRowException extends RuntimeException {
        private final long rowNumber;

        public CsvRowException(long rowNumber, String message) {
            super(message);
            this.rowNumber = rowNumber;
        }

        public long getRowNumber() {
            return rowNumber;
        }

        @Override
        public String getMessage() {
            return "Row " + rowNumber + ": " + super.getMessage();
        }
    }
}
