package dev.reagentic.ai.agent;

import java.util.List;

public record ClassifyRequest(List<TransactionInput> transactions) {
}
