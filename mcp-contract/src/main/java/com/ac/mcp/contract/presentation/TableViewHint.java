package com.ac.mcp.contract.presentation;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TableViewHint(String id, String type, String subType, String title, String description,
                            TableMapping mapping, Map<String, Object> options, Integer priority,
                            DrillDown drillDown)
        implements ViewHint {
    public TableViewHint {
        type = "table";
        mapping = mapping == null ? TableMapping.auto() : mapping;
        options = options == null ? Map.of() : Map.copyOf(options);
        priority = priority == null ? 0 : priority;
        drillDown = drillDown == null ? DrillDown.disabled() : drillDown;
    }

    public TableViewHint(String id, String type, String subType, String title, String description,
                         Map<String, String> mapping, Map<String, Object> options, Integer priority) {
        this(id, type, subType, title, description, TableMapping.from(mapping), options, priority, null);
    }
}
