package com.ac.mcp.contract.presentation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Runtime-neutral, declarative component description. */
public record ComponentSpec(String id, String type, Map<String, Object> props) {

    public ComponentSpec {
        id = requireText(id, "id");
        type = requireText(type, "type");
        props = props == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(props));
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
