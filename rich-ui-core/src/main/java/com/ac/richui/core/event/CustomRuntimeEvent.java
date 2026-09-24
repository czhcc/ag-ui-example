package com.ac.richui.core.event;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Runtime-neutral custom event for explicitly allow-listed protocol extensions. */
public record CustomRuntimeEvent(
        String name,
        Map<String, Object> value,
        Instant timestamp) implements RichRuntimeEvent {
    public CustomRuntimeEvent {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        value = value == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(value));
        timestamp = timestamp == null ? Instant.now() : timestamp;
    }

    public CustomRuntimeEvent(String name, Map<String, Object> value) {
        this(name, value, Instant.now());
    }

    @Override public String type() { return name; }
}
