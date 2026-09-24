package com.ac.richui.core.event;

import com.ac.mcp.contract.presentation.SurfaceSpec;

import java.time.Instant;
import java.util.Objects;

/**
 * 表示 Rich UI Surface 已创建的内部运行事件。
 */
public record SurfaceCreated(SurfaceSpec surface, Instant timestamp) implements RichRuntimeEvent {
    /**
     * 校验 Surface 并补齐事件时间。
     */
    public SurfaceCreated {
        Objects.requireNonNull(surface, "surface must not be null");
        timestamp = timestamp == null ? Instant.now() : timestamp;
    }

    /**
     * 使用当前时间创建 Surface 事件。
     */
    public SurfaceCreated(SurfaceSpec surface) {
        this(surface, Instant.now());
    }

    /**
     * 返回 Surface 创建事件的协议名称。
     */
    @Override
    public String type() {
        return "ui.surface.create";
    }
}
