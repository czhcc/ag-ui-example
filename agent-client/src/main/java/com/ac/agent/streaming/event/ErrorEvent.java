package com.ac.agent.streaming.event;
import com.ac.agent.streaming.*;
import java.time.Instant;
public record ErrorEvent(String code, String message, Instant timestamp) implements AgentEvent {
    public ErrorEvent(String code, String message) { this(code, message, Instant.now()); }
    @Override public AgentEventType type() { return AgentEventType.ERROR; }
}
