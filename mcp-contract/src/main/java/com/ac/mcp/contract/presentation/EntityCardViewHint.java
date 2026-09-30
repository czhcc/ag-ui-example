package com.ac.mcp.contract.presentation;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/** EntityCard presentation hint. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EntityCardViewHint(String id, String type, String subType, String title, String description,
        EntityCardMapping mapping, Map<String, Object> options, Integer priority, DrillDown drillDown)
        implements ViewHint {
    public EntityCardViewHint {
        type = "entity_card";
        subType = "standard";
        mapping = java.util.Objects.requireNonNull(mapping, "mapping must not be null");
        options = options == null ? Map.of() : Map.copyOf(options);
        priority = priority == null ? 0 : priority;
        drillDown = drillDown == null ? DrillDown.disabled() : drillDown;
    }
}
