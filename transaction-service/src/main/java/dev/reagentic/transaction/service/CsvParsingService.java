package dev.reagentic.transaction.service;

import dev.reagentic.transaction.model.Transaction;
import dev.reagentic.transaction.util.CsvRowMapper;
import lombok.Getter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvParsingService {

    public ParseResult parse(MultipartFile file, String accountId, String uploadBatchId) {
        List<Transaction> parsed = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .build();

        try (var reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, format)) {

            parser.forEach(record -> {
                try {
                    parsed.add(CsvRowMapper.map(record, accountId, uploadBatchId));
                } catch (CsvRowMapper.CsvRowException e) {
                    errors.add(e.getMessage());
                }
            });

        } catch (IOException e) {
            throw new CsvParsingFailedException("Could not read uploaded file: " + e.getMessage(), e);
        }

        return new ParseResult(parsed, errors);
    }

    public static class ParseResult {
        private final List<Transaction> transactions;
        private final List<String> errors;

        public ParseResult(List<Transaction> transactions, List<String> errors) {
            this.transactions = transactions;
            this.errors = errors;
        }

        public List<Transaction> getTransactions() {
            return transactions;
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    public static class CsvParsingFailedException extends RuntimeException {
        public CsvParsingFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
