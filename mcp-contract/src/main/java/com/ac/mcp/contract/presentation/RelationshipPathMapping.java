package com.ac.mcp.contract.presentation;

import java.util.Set;

/** RelationshipPath business-field mapping. */
public record RelationshipPathMapping(String step, String source, String target, String label) implements ViewMapping {
    public RelationshipPathMapping {
        step = MappingFields.required(step, "step");
        source = MappingFields.required(source, "source");
        target = MappingFields.required(target, "target");
        label = MappingFields.optional(label);
    }

    @Override public Set<String> referencedFields() {
        return MappingFields.of(step, source, target, label);
    }
}
