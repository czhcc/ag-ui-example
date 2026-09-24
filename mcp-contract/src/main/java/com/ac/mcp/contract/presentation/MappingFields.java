package com.ac.mcp.contract.presentation;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

final class MappingFields {
    private MappingFields() { }

    static String required(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    static String optional(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    static Set<String> of(String... values) {
        var fields = new LinkedHashSet<String>();
        for (String value : values) {
            if (value != null) {
                fields.add(value);
            }
        }
        return Collections.unmodifiableSet(fields);
    }
}
