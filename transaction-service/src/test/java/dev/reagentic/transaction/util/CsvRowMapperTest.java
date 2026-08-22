package dev.reagentic.transaction.util;

import dev.reagentic.transaction.model.Transaction;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvRowMapperTest {

    private List<org.apache.commons.csv.CSVRecord> parse(String csv) throws Exception {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader().setSkipHeaderRecord(true).setTrim(true).build();
        try (CSVParser parser = new CSVParser(new StringReader(csv), format)) {
            return parser.getRecords();
        }
    }

    @Test
    void mapsAValidRow() throws Exception {
        String csv = "date,description,amount,category\n2026-08-01,Jollibee,-350.00,Dining\n";
        var record = parse(csv).get(0);

        Transaction t = CsvRowMapper.map(record, "acc-checking-0001", "batch-1");

        assertEquals("acc-checking-0001", t.getAccountId());
        assertEquals(new BigDecimal("-350.00"), t.getAmount());
        assertEquals("Jollibee", t.getDescription());
        assertEquals("Dining", t.getRawCategory());
    }

    @Test
    void handlesPhpCurrencySymbolAndCommas() throws Exception {
        String csv = "date,description,amount,category\n2026-08-01,Salary,\"₱25,000.00\",Income\n";
        var record = parse(csv).get(0);

        Transaction t = CsvRowMapper.map(record, "acc-checking-0001", "batch-1");

        assertEquals(new BigDecimal("25000.00"), t.getAmount());
    }

    @Test
    void rejectsMissingAmount() throws Exception {
        String csv = "date,description,amount,category\n2026-08-01,Mystery charge,,Unknown\n";
        var record = parse(csv).get(0);

        assertThrows(CsvRowMapper.CsvRowException.class,
                () -> CsvRowMapper.map(record, "acc-checking-0001", "batch-1"));
    }

    @Test
    void rejectsUnparseableDate() throws Exception {
        String csv = "date,description,amount,category\nnot-a-date,Jollibee,-100.00,Dining\n";
        var record = parse(csv).get(0);

        assertThrows(CsvRowMapper.CsvRowException.class,
                () -> CsvRowMapper.map(record, "acc-checking-0001", "batch-1"));
    }
}
