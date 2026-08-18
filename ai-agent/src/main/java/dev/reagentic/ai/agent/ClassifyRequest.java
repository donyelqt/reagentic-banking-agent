package dev.reagentic.ai.agent;

import java.util.List;
import java.util.regex.Pattern;

public record ClassifyRequest(List<TransactionInput> transactions) {

    public static final int MAX_ITEMS = 100;
    public static final int MAX_DESCRIPTION_LENGTH = 200;
    public static final int MAX_AMOUNT_LENGTH = 32;

    private static final Pattern AMOUNT = Pattern.compile("[+-]?\\d+(\\.\\d{1,2})?");

    /**
     * Boundary validation for untrusted client input. Returns an error message
     * describing the first problem found, or {@code null} when the request is
     * acceptable. Rejecting malformed input at the edge keeps silent data
     * corruption (e.g. a garbage amount quietly classifying as zero) out of the
     * classification pipeline.
     */
    public static String validate(ClassifyRequest req) {
        if (req == null || req.transactions() == null) {
            return "transactions must not be null";
        }
        List<TransactionInput> items = req.transactions();
        if (items.size() > MAX_ITEMS) {
            return "transactions list exceeds the maximum of " + MAX_ITEMS + " items";
        }
        for (int i = 0; i < items.size(); i++) {
            TransactionInput t = items.get(i);
            if (t == null) {
                return "transactions[" + i + "] must not be null";
            }
            if (t.description() == null || t.description().isBlank()) {
                return "transactions[" + i + "].description must not be blank";
            }
            if (t.description().length() > MAX_DESCRIPTION_LENGTH) {
                return "transactions[" + i + "].description exceeds " + MAX_DESCRIPTION_LENGTH + " characters";
            }
            if (t.amount() == null || t.amount().isBlank()) {
                return "transactions[" + i + "].amount must not be blank";
            }
            if (t.amount().length() > MAX_AMOUNT_LENGTH) {
                return "transactions[" + i + "].amount exceeds " + MAX_AMOUNT_LENGTH + " characters";
            }
            String normalized = t.amount().replace(",", "");
            if (!AMOUNT.matcher(normalized).matches()) {
                return "transactions[" + i + "].amount '" + t.amount() + "' is not a valid amount";
            }
        }
        return null;
    }
}
