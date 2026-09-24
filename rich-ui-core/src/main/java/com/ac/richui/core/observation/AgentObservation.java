package com.ac.richui.core.observation;

import java.util.Objects;

/** Bounded, sanitized text that may be returned to an agent runtime. */
public record AgentObservation(String content) {

    public AgentObservation {
        Objects.requireNonNull(content, "content must not be null");
    }
}
