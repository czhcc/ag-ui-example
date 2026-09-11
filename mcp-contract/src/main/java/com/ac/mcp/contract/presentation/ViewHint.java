package com.ac.mcp.contract.presentation;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Map;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ChartViewHint.class, name = "chart"),
        @JsonSubTypes.Type(value = RelationGraphViewHint.class, name = "relation_graph"),
        @JsonSubTypes.Type(value = TimelineViewHint.class, name = "timeline"),
        @JsonSubTypes.Type(value = TableViewHint.class, name = "table")
})
public sealed interface ViewHint permits ChartViewHint, RelationGraphViewHint, TimelineViewHint, TableViewHint {
    String id();
    String type();
    String subType();
    String title();
    String description();
    Map<String, String> mapping();
    Map<String, Object> options();
    Integer priority();
    DrillDown drillDown();
}
