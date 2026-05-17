package dev.abstratium.abstoggle.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record ClientContextEntry(
    String key,
    String value
) {
}
