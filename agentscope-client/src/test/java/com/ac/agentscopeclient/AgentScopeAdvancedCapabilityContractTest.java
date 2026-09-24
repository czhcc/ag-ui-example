package com.ac.agentscopeclient;

import com.ac.agentscopeclient.config.AgentScopeDistributedStateGuard;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.agui.adapter.AguiAdapterConfig;
import io.agentscope.core.agui.adapter.AguiAgentAdapter;
import io.agentscope.core.agui.model.RunAgentInput;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.harness.agent.HarnessAgent;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the extension points used by Harness/subagent/HITL/distributed-state deployments. */
class AgentScopeAdvancedCapabilityContractTest {
    @Test
    void harnessAndDistributedStateUseTheSameAgentAndRuntimeContextBoundaries() throws Exception {
        assertTrue(Agent.class.isAssignableFrom(HarnessAgent.class));
        assertTrue(ReActAgent.Builder.class.getMethod("stateStore", AgentStateStore.class) != null);
        assertTrue(AguiAgentAdapter.class.getMethod(
                "run", RunAgentInput.class, RuntimeContext.class).getReturnType().equals(Flux.class));
    }

    @Test
    void subagentEventsRemainScopedCustomEventsByDefault() {
        assertFalse(AguiAdapterConfig.defaultConfig().isEmitSubagentEventsAsNative());
    }

    @Test
    void distributedModeRejectsProcessLocalAgentState() {
        assertThrows(IllegalStateException.class,
                () -> new AgentScopeDistributedStateGuard(true, new InMemoryAgentStateStore()));
    }
}
