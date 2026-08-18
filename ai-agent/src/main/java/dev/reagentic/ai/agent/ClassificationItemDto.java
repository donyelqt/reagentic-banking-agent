package dev.reagentic.ai.agent;

/** JSON shape one classified item takes in the LLM's response. */
public record ClassificationItemDto(String description, String category) {
}
