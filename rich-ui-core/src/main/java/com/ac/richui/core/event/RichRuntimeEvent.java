package com.ac.richui.core.event;

import java.time.Instant;

/**
 * 内部事件契约，仅暴露项目或 JDK 类型，避免泄露 Runtime 原生事件对象。
 */
public interface RichRuntimeEvent {

    /**
     * 返回事件类型。
     */
    String type();

    /**
     * 返回事件发生时间。
     */
    Instant timestamp();
}
