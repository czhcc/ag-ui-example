package com.ac.mcp.contract.presentation;

import java.util.Map;
import java.util.Set;

public record ChartMapping(String category, String value) implements ViewMapping {

    public ChartMapping {
        category = MappingFields.required(category, "category");
        value = MappingFields.required(value, "value");
    }

    public static ChartMapping from(Map<String, String> mapping) {
        return new ChartMapping(mapping == null ? null : mapping.get("category"),
                mapping == null ? null : mapping.get("value"));
    }

    @Override
    public Set<String> referencedFields() {
        return MappingFields.of(category, value);
    }
}
