package com.ac.mcp.contract.presentation;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RelationGraphMapping(
        String source,
        String target,
        String label,
        String sourceLabel) implements ViewMapping {

    public RelationGraphMapping {
        source = MappingFields.required(source, "source");
        target = MappingFields.required(target, "target");
        label = MappingFields.optional(label);
        sourceLabel = MappingFields.optional(sourceLabel);
    }

    public static RelationGraphMapping from(Map<String, String> mapping) {
        return new RelationGraphMapping(
                mapping == null ? null : mapping.get("source"),
                mapping == null ? null : mapping.get("target"),
                mapping == null ? null : mapping.get("label"),
                mapping == null ? null : mapping.get("sourceLabel"));
    }

    @Override
    public Set<String> referencedFields() {
        return MappingFields.of(source, target, label, sourceLabel);
    }
}
