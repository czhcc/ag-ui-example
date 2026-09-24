package com.ac.runtime.agentscope;

import com.ac.richui.core.context.RunScope;
import io.agentscope.core.event.CustomEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/** Mutable, call-local bridge carried as a typed AgentScope RuntimeContext attribute. */
public final class AgentScopeRunContext {
    private final RunScope scope;
    private final Queue<CustomEvent> pendingEvents = new ConcurrentLinkedQueue<>();
    private final Consumer<CustomEvent> eventConsumer;

    public AgentScopeRunContext(RunScope scope) {
        this(scope, null);
    }

    public AgentScopeRunContext(RunScope scope, Consumer<CustomEvent> eventConsumer) {
        this.scope = Objects.requireNonNull(scope, "scope must not be null");
        this.eventConsumer = eventConsumer;
    }

    public RunScope scope() {
        return scope;
    }

    public void emit(String name, Map<String, Object> value) {
        CustomEvent event = new CustomEvent(name, value);
        if (eventConsumer != null) {
            eventConsumer.accept(event);
        } else {
            pendingEvents.add(event);
        }
    }

    public List<CustomEvent> drainEvents() {
        List<CustomEvent> drained = new ArrayList<>();
        CustomEvent event;
        while ((event = pendingEvents.poll()) != null) drained.add(event);
        return List.copyOf(drained);
    }
}
