package dev.reagentic.ai.agent;

import java.util.List;

/** JSON shape the LLM classifier must fill: one category per input transaction, in order. */
public record ClassificationDto(List<ClassificationItemDto> items) {
}
