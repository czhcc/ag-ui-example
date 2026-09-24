package com.ac.richui.core.event;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 与运行时无关的自定义事件，用于协议层明确允许的扩展。
 */
public record CustomRuntimeEvent(
        String name,
        Map<String, Object> value,
        Instant timestamp) implements RichRuntimeEvent {
    /**
     * 校验事件名称，并补齐载荷和时间戳。
     */
    public CustomRuntimeEvent {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        value = value == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(value));
        timestamp = timestamp == null ? Instant.now() : timestamp;
    }

    /**
     * 使用当前时间创建自定义事件。
     */
    public CustomRuntimeEvent(String name, Map<String, Object> value) {
        this(name, value, Instant.now());
    }

    /**
     * 返回作为事件类型的自定义名称。
     */
    @Override
    public String type() {
        return name;
    }
}
