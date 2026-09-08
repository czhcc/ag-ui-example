package com.ac.agent.streaming.event;
import com.ac.agent.streaming.*;
import java.time.Instant;
public record TextStartEvent(String messageId, Instant timestamp) implements AgentEvent {
    public TextStartEvent(String messageId) { this(messageId, Instant.now()); }
    @Override public AgentEventType type() { return AgentEventType.TEXT_MESSAGE_START; }
}
