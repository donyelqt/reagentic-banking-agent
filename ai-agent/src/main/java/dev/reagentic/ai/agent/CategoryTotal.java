package dev.reagentic.ai.agent;

/** Aggregated spend for one category: total signed amount and transaction count. */
public record CategoryTotal(SpendingCategory category, String total, int count) {
}
