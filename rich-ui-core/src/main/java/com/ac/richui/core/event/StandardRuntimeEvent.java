package com.ac.richui.core.event;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 与运行时无关的生命周期事件，携带受控的属性载荷。
 */
public record StandardRuntimeEvent(
        String type,
        Map<String, Object> attributes,
        Instant timestamp) implements RichRuntimeEvent {

    /**
     * 校验事件类型，并补齐属性和时间戳。
     */
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

    /**
     * 使用当前时间创建带属性的标准事件。
     */
    public StandardRuntimeEvent(String type, Map<String, Object> attributes) {
        this(type, attributes, Instant.now());
    }

    /**
     * 创建不带属性的标准事件。
     */
    public static StandardRuntimeEvent of(String type) {
        return new StandardRuntimeEvent(type, Map.of());
    }
}
