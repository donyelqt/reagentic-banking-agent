package dev.reagentic.ai.agent;

/**
 * A single transaction to classify: free-text description (e.g. merchant name or
 * memo) plus its signed decimal amount as a string (negative = money out).
 */
public record TransactionInput(String description, String amount) {
}
