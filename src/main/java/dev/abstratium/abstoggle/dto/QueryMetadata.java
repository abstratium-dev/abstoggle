package dev.abstratium.abstoggle.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class QueryMetadata {
    private String stage;
    private String nameFilter;
    private Integer count;
    private Boolean cacheHit;

    public QueryMetadata() {}

    public QueryMetadata(String stage, String nameFilter, Integer count, Boolean cacheHit) {
        this.stage = stage;
        this.nameFilter = nameFilter;
        this.count = count;
        this.cacheHit = cacheHit;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getNameFilter() {
        return nameFilter;
    }

    public void setNameFilter(String nameFilter) {
        this.nameFilter = nameFilter;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Boolean getCacheHit() {
        return cacheHit;
    }

    public void setCacheHit(Boolean cacheHit) {
        this.cacheHit = cacheHit;
    }
}
