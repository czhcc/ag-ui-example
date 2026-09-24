package com.ac.mcp.contract.presentation;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChartViewHint(String id, String type, String subType, String title, String description,
                            ChartMapping mapping, Map<String, Object> options, Integer priority,
                            DrillDown drillDown)
        implements ViewHint {
    public ChartViewHint {
        type = "chart";
        mapping = java.util.Objects.requireNonNull(mapping, "mapping must not be null");
        options = options == null ? Map.of() : Map.copyOf(options);
        priority = priority == null ? 0 : priority;
        drillDown = drillDown == null ? DrillDown.disabled() : drillDown;
    }

    public ChartViewHint(String id, String type, String subType, String title, String description,
                         Map<String, String> mapping, Map<String, Object> options, Integer priority) {
        this(id, type, subType, title, description, ChartMapping.from(mapping), options, priority, null);
    }
}
