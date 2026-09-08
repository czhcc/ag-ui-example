package com.ac.agent.streaming.event;
import com.ac.agent.streaming.*;
import java.time.Instant;
public record RunFinishedEvent(String runId, Instant timestamp) implements AgentEvent {
    public RunFinishedEvent(String runId) { this(runId, Instant.now()); }
    @Override public AgentEventType type() { return AgentEventType.RUN_FINISHED; }
}
