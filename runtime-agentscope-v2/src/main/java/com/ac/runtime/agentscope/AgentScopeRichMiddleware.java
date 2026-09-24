package com.ac.runtime.agentscope;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.middleware.ActingInput;
import io.agentscope.core.middleware.AgentInput;
import io.agentscope.core.middleware.MiddlewareBase;
import java.util.function.Function;
import reactor.core.publisher.Flux;

/** Enforces trusted ownership and releases tool-produced CustomEvents into the agent event stream. */
public final class AgentScopeRichMiddleware implements MiddlewareBase {
    @Override
    public Flux<AgentEvent> onAgent(
            Agent agent, RuntimeContext context, AgentInput input,
            Function<AgentInput, Flux<AgentEvent>> next) {
        AgentScopeRuntimeContexts.require(context);
        return next.apply(input);
    }

    @Override
    public Flux<AgentEvent> onActing(
            Agent agent, RuntimeContext context, ActingInput input,
            Function<ActingInput, Flux<AgentEvent>> next) {
        AgentScopeRunContext run = AgentScopeRuntimeContexts.require(context);
        return next.apply(input)
                .concatWith(Flux.defer(() -> Flux.fromIterable(run.drainEvents()).cast(AgentEvent.class)));
    }
}
