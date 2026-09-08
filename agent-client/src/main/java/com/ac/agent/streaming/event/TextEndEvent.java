package com.ac.agent.streaming.event;
import com.ac.agent.streaming.*;
import java.time.Instant;
public record TextEndEvent(String messageId, Instant timestamp) implements AgentEvent {
    public TextEndEvent(String messageId) { this(messageId, Instant.now()); }
    @Override public AgentEventType type() { return AgentEventType.TEXT_MESSAGE_END; }
}
