package com.ac.richui.core.event;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Runtime-neutral lifecycle event with a deliberately safe, bounded payload. */
public record StandardRuntimeEvent(
        String type,
        Map<String, Object> attributes,
        Instant timestamp) implements RichRuntimeEvent {

    public StandardRuntimeEvent {
        Objects.requireNonNull(type, "type must not be null");
        if (type.isBlank()) {
            throw new IllegalArgumentException("type must not be blank");
        }
        attributes = attributes == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
        timestamp = timestamp == null ? Instant.now() : timestamp;
    }

    public StandardRuntimeEvent(String type, Map<String, Object> attributes) {
        this(type, attributes, Instant.now());
    }

    public static StandardRuntimeEvent of(String type) {
        return new StandardRuntimeEvent(type, Map.of());
    }
}
