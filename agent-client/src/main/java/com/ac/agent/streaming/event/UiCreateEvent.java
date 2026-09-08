package com.ac.agent.streaming.event;
import com.ac.agent.presentation.model.SurfaceSpec;
import com.ac.agent.streaming.*;
import java.time.Instant;
public record UiCreateEvent(SurfaceSpec surface, Instant timestamp) implements AgentEvent {
    public UiCreateEvent(SurfaceSpec surface) { this(surface, Instant.now()); }
    @Override public AgentEventType type() { return AgentEventType.UI_CREATE; }
}
