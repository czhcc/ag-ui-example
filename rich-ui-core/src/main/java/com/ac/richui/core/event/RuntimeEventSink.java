package com.ac.richui.core.event;

import com.ac.richui.core.context.RunScope;

/**
 * 按运行范围发布事件，不依赖 SSE 或具体 Runtime SDK。
 */
@FunctionalInterface
public interface RuntimeEventSink {

    /**
     * 将事件发布到指定运行的事件流。
     */
    void publish(RunScope scope, RichRuntimeEvent event);
}
