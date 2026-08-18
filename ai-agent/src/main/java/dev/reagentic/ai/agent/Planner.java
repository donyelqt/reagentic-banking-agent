package dev.reagentic.ai.agent;

import java.util.List;

public interface Planner {
    /**
     * @param message the current user request
     * @param history prior conversation turns, oldest first (may be null/empty); implementations
     *                that don't need context (e.g. a deterministic keyword planner) may ignore it
     */
    Plan plan(String message, List<String> history);
}
