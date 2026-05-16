package dev.abstratium.abstoggle.dto;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record QueryResponse(
    List<QueryTSRDto> toggles,
    QueryMetadata queryMetadata
) {

    public QueryResponse() {
        this(List.of(), new QueryMetadata());
    }
}