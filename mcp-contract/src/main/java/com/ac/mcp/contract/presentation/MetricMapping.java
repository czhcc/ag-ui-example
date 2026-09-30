package com.ac.mcp.contract.presentation;

import java.util.Set;

/** Metric business-field mapping. */
public record MetricMapping(String label, String value, String unit, String change) implements ViewMapping {
    public MetricMapping {
        label = MappingFields.required(label, "label");
        value = MappingFields.required(value, "value");
        unit = MappingFields.optional(unit);
        change = MappingFields.optional(change);
    }

    @Override public Set<String> referencedFields() {
        return MappingFields.of(label, value, unit, change);
    }
}
