package com.ac.mcp.contract.presentation;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/** Metric presentation hint. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MetricViewHint(String id, String type, String subType, String title, String description,
        MetricMapping mapping, Map<String, Object> options, Integer priority, DrillDown drillDown)
        implements ViewHint {
    public MetricViewHint {
        type = "metric";
        subType = "single";
        mapping = java.util.Objects.requireNonNull(mapping, "mapping must not be null");
        options = options == null ? Map.of() : Map.copyOf(options);
        priority = priority == null ? 0 : priority;
        drillDown = drillDown == null ? DrillDown.disabled() : drillDown;
    }
}
