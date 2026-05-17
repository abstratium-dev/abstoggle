package dev.abstratium.abstoggle.dto;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record EvaluatorResponse(
    List<EvaluatorResultDto> results,
    String stage,
    String nameFilter,
    List<ClientContextEntry> context,
    Boolean cacheHit,
    Boolean cacheEnabled,
    Integer cacheTtlSeconds
) {
}
