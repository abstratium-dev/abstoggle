package dev.abstratium.abstoggle.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record EvaluatorResultDto(
    String toggleName,
    String resolvedValue,
    String debug
) {
}
