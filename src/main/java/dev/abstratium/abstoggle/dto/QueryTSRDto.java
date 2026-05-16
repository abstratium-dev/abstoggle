package dev.abstratium.abstoggle.dto;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record QueryTSRDto (
    // toggle
    String toggleName,
    String toggleDescription,
    Boolean toggleEnabled,
    String toggleContext,

    // stage
    String stageName,

    // rule
    String ruleName,
    String ruleDescription,
    List<CriterionDto> ruleCriteria,

    // tsr
    Integer priority,
    String value
) {
}
