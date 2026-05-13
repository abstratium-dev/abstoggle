package dev.abstratium.abstoggle.dto;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ToggleQueryResponse {
    private List<ToggleDto> toggles;
    private QueryMetadata queryMetadata;

    public ToggleQueryResponse() {}

    public ToggleQueryResponse(List<ToggleDto> toggles, QueryMetadata queryMetadata) {
        this.toggles = toggles;
        this.queryMetadata = queryMetadata;
    }

    public List<ToggleDto> getToggles() {
        return toggles;
    }

    public void setToggles(List<ToggleDto> toggles) {
        this.toggles = toggles;
    }

    public QueryMetadata getQueryMetadata() {
        return queryMetadata;
    }

    public void setQueryMetadata(QueryMetadata queryMetadata) {
        this.queryMetadata = queryMetadata;
    }
}
