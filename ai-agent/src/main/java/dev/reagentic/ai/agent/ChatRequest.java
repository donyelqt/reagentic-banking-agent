package dev.reagentic.ai.agent;

import java.util.List;

public record ChatRequest(
        String message,
        List<String> history,
        List<Step> plan,
        List<String> approval,
        String jwt) {
}
