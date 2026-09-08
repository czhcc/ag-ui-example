package com.ac.agent.streaming;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class DefaultAgentEventBus implements AgentEventBus {
    private final Sinks.Many<AgentEvent> sink = Sinks.many().multicast().onBackpressureBuffer(1024, false);
    @Override public void emit(AgentEvent event) {
        var result = sink.tryEmitNext(event);
        if (result.isFailure()) throw new IllegalStateException("Unable to publish agent event: " + result);
    }
    @Override public Flux<AgentEvent> events() { return sink.asFlux(); }
}
