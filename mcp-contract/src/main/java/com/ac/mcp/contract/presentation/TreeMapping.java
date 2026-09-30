package com.ac.mcp.contract.presentation;

import java.util.Set;

/** Tree business-field mapping. */
public record TreeMapping(String id, String parentId, String label, String hasChildren) implements ViewMapping {
    public TreeMapping {
        id = MappingFields.required(id, "id");
        parentId = MappingFields.required(parentId, "parentId");
        label = MappingFields.required(label, "label");
        hasChildren = MappingFields.optional(hasChildren);
    }

    @Override public Set<String> referencedFields() {
        return MappingFields.of(id, parentId, label, hasChildren);
    }
}
