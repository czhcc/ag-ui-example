package com.ac.richui.core.event;

import com.ac.richui.core.context.RunScope;

/** Publishes into a run-scoped stream without depending on SSE or a runtime SDK. */
@FunctionalInterface
public interface RuntimeEventSink {

    void publish(RunScope scope, RichRuntimeEvent event);
}
