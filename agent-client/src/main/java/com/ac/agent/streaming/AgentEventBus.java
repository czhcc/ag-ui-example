package com.ac.agent.streaming;

import reactor.core.publisher.Flux;

public interface AgentEventBus {
    void emit(AgentEvent event);
    Flux<AgentEvent> events();
}
