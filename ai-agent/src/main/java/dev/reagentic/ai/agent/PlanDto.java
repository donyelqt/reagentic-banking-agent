package dev.reagentic.ai.agent;

import java.util.List;

/**
 * JSON shape the LLM planner must fill. Kept separate from the runtime
 * {@link Step} so the model only has to provide the intent-carrying fields.
 */
public record PlanDto(List<StepDto> steps) {
}