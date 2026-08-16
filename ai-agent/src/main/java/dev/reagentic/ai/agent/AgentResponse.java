package dev.reagentic.ai.agent;

import java.util.List;

public record AgentResponse(
        List<Step> plan,
        List<StepResult> results,
        List<Step> pendingSteps,
        String reply) {
}
