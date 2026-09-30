package com.ac.mcp.contract.presentation;

import java.util.Set;

/** Heatmap business-field mapping. */
public record HeatmapMapping(String x, String y, String value) implements ViewMapping {
    public HeatmapMapping {
        x = MappingFields.required(x, "x");
        y = MappingFields.required(y, "y");
        value = MappingFields.required(value, "value");
    }

    @Override public Set<String> referencedFields() {
        return MappingFields.of(x, y, value);
    }
}
