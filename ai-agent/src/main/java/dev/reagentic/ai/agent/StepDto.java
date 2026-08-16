package dev.reagentic.ai.agent;

import java.util.List;
import java.util.Map;

public record StepDto(
        String stepId,
        String tool,
        Map<String, Object> args,
        List<String> dependsOn,
        boolean confirmationRequired,
        String idempotencyKey) {
}