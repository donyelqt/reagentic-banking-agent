package dev.reagentic.ai.agent;

import java.util.List;
import java.util.Map;

public record AgentResponse(
        List<Step> plan,
        List<StepResult> results,
        List<Step> pendingSteps,
        String reply) {
}
