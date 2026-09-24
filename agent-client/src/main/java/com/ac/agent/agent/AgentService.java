package com.ac.agent.agent;

import com.ac.runtime.saa.HitlDecision;
import com.ac.runtime.saa.PendingHumanInput;
import com.ac.runtime.saa.SaaAgentRuntime;
import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Service
public final class AgentService {
    private final AgentFactory factory;
    private final com.ac.richui.core.event.RuntimeEventSink events;
    private volatile SaaAgentRuntime runtime;

    public AgentService(AgentFactory factory, com.ac.richui.core.event.RuntimeEventSink events) {
        this.factory = factory;
        this.events = events;
    }

    public Flux<String> stream(String message, AgentContext context) {
        return Flux.defer(() -> runtime().stream(message, context.scope()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public boolean cancel(AgentContext context) {
        SaaAgentRuntime current = runtime;
        return current != null && current.cancel(context.scope());
    }

    public Flux<String> resume(AgentContext context, List<HitlDecision> decisions) {
        return Flux.defer(() -> runtime().resume(context.scope(), decisions))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Flux<String> resume(AgentContext interrupted, AgentContext output, List<HitlDecision> decisions) {
        return Flux.defer(() -> runtime().resume(interrupted.scope(), output.scope(), decisions))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public PendingHumanInput pending(AgentContext context) {
        SaaAgentRuntime current = runtime;
        return current == null ? null : current.pending(context.scope());
    }

    private SaaAgentRuntime runtime() {
        SaaAgentRuntime current = runtime;
        if (current != null) return current;
        synchronized (this) {
            if (runtime == null) runtime = new SaaAgentRuntime(factory.create(), events);
            return runtime;
        }
    }
}
