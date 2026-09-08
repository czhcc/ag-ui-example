package com.ac.agent.streaming.event;
import com.ac.agent.streaming.*;
import java.time.Instant;
public record ToolEndEvent(String toolCallId, String toolName, boolean success, String resultRef, Instant timestamp) implements AgentEvent {
    public ToolEndEvent(String id, String name, boolean success, String ref) { this(id, name, success, ref, Instant.now()); }
    @Override public AgentEventType type() { return AgentEventType.TOOL_CALL_END; }
}
