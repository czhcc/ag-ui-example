package com.ac.mcp.contract.presentation;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TimelineMapping(
        String time,
        String title,
        String description,
        String group) implements ViewMapping {

    public TimelineMapping {
        time = MappingFields.required(time, "time");
        title = MappingFields.required(title, "title");
        description = MappingFields.optional(description);
        group = MappingFields.optional(group);
    }

    public static TimelineMapping from(Map<String, String> mapping) {
        return new TimelineMapping(
                mapping == null ? null : mapping.get("time"),
                mapping == null ? null : mapping.get("title"),
                mapping == null ? null : mapping.get("description"),
                mapping == null ? null : mapping.get("group"));
    }

    @Override
    public Set<String> referencedFields() {
        return MappingFields.of(time, title, description, group);
    }
}
