package com.ac.runtime.agentscope;

import com.ac.richui.core.context.RunScope;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.CustomEvent;
import java.util.Objects;
import java.util.function.Consumer;

/** Creates and validates the trusted RuntimeContext-to-RunScope mapping. */
public final class AgentScopeRuntimeContexts {
    private AgentScopeRuntimeContexts() { }

    public static RuntimeContext create(RunScope scope) {
        return create(scope, null);
    }

    public static RuntimeContext create(RunScope scope, Consumer<CustomEvent> eventConsumer) {
        Objects.requireNonNull(scope, "scope must not be null");
        AgentScopeRunContext run = new AgentScopeRunContext(scope, eventConsumer);
        return RuntimeContext.builder()
                .sessionId(scope.threadId())
                .userId(scope.userId())
                .put(AgentScopeRunContext.class, run)
                .put("ac.tenantId", scope.tenantId())
                .put("ac.runId", scope.runId())
                .build();
    }

    public static AgentScopeRunContext require(RuntimeContext context) {
        if (context == null) throw new IllegalStateException("AgentScope RuntimeContext is required");
        AgentScopeRunContext run = context.get(AgentScopeRunContext.class);
        if (run == null) throw new IllegalStateException("Trusted RunScope is missing from RuntimeContext");
        RunScope scope = run.scope();
        if (!scope.threadId().equals(context.getSessionId()) || !scope.userId().equals(context.getUserId())) {
            throw new IllegalStateException("AgentScope session ownership does not match RunScope");
        }
        return run;
    }

    public static RunScope requireScope(RuntimeContext context, String toolCallId) {
        return require(context).scope().withToolCallId(toolCallId);
    }
}
