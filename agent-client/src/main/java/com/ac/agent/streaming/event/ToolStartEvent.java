package com.ac.agent.streaming.event;
import com.ac.agent.streaming.*;
import java.time.Instant;
public record ToolStartEvent(String toolCallId, String toolName, Instant timestamp) implements AgentEvent {
    public ToolStartEvent(String toolCallId, String toolName) { this(toolCallId, toolName, Instant.now()); }
    @Override public AgentEventType type() { return AgentEventType.TOOL_CALL_START; }
}
