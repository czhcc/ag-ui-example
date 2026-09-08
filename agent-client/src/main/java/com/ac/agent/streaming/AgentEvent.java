package com.ac.agent.streaming;

import java.time.Instant;

public interface AgentEvent {
    AgentEventType type();
    Instant timestamp();
}
