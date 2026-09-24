package com.ac.agentscopeclient.agui;

import com.ac.agui.protocol.AgUiRunAgentInput;
import com.ac.agui.web.AgUiRunHandler;
import com.ac.richui.core.context.RunScope;
import com.ac.runtime.agentscope.AgentScopeAgUiRuntime;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public final class AgentScopeAgUiRunHandler implements AgUiRunHandler {
    private final AgentScopeAgUiRuntime runtime;

    public AgentScopeAgUiRunHandler(AgentScopeAgUiRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public Mono<Void> execute(AgUiRunAgentInput input, RunScope scope) {
        return runtime.execute(input, scope);
    }

    @Override
    public boolean cancel(RunScope scope) {
        return runtime.cancel(scope);
    }
}
