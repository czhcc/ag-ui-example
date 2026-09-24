package com.ac.richui.core.event;

import java.time.Instant;

/**
 * Marker contract for internal events. Implementations must contain only
 * project-owned or JDK types and must not expose native runtime event objects.
 */
public interface RichRuntimeEvent {

    String type();

    Instant timestamp();
}
