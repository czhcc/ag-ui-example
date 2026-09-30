package com.ac.mcp.contract.presentation;

import java.util.Set;

/** EvidenceChain business-field mapping. */
public record EvidenceChainMapping(String step, String id, String title, String source, String summary, String evidenceRef) implements ViewMapping {
    public EvidenceChainMapping {
        step = MappingFields.required(step, "step");
        id = MappingFields.required(id, "id");
        title = MappingFields.required(title, "title");
        source = MappingFields.required(source, "source");
        summary = MappingFields.optional(summary);
        evidenceRef = MappingFields.optional(evidenceRef);
    }

    @Override public Set<String> referencedFields() {
        return MappingFields.of(step, id, title, source, summary, evidenceRef);
    }
}
