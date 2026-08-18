package dev.reagentic.ai.agent;

import java.util.List;

public record ClassifyResponse(List<ClassifiedTransaction> transactions, List<CategoryTotal> summary) {
}
