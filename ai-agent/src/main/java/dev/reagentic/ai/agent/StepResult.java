package dev.reagentic.ai.agent;

public record StepResult(
        String stepId,
        boolean ok,
        Object data,
        String error) {
}
