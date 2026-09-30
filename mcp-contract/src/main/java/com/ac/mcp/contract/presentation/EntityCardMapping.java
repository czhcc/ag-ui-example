package com.ac.mcp.contract.presentation;

import java.util.Set;

/** EntityCard business-field mapping. */
public record EntityCardMapping(String id, String name, String kind, String summary) implements ViewMapping {
    public EntityCardMapping {
        id = MappingFields.required(id, "id");
        name = MappingFields.required(name, "name");
        kind = MappingFields.optional(kind);
        summary = MappingFields.optional(summary);
    }

    @Override public Set<String> referencedFields() {
        return MappingFields.of(id, name, kind, summary);
    }
}
