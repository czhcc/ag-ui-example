package com.ac.agent.streaming.event;
import com.ac.agent.streaming.*;
import java.time.Instant;
public record TextDeltaEvent(String messageId, String delta, Instant timestamp) implements AgentEvent {
    public TextDeltaEvent(String messageId, String delta) { this(messageId, delta, Instant.now()); }
    @Override public AgentEventType type() { return AgentEventType.TEXT_MESSAGE_CONTENT; }
}
