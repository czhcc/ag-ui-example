package com.ac.agentscopeclient.config;

import io.agentscope.core.state.AgentStateStore;

/** Fails fast when multi-replica mode would otherwise keep AgentScope state in one JVM. */
public final class AgentScopeDistributedStateGuard {
    public AgentScopeDistributedStateGuard(boolean distributed, AgentStateStore stateStore) {
        if (distributed && !(stateStore instanceof SharedAgentStateStore)) {
            throw new IllegalStateException(
                    "Distributed mode requires a SharedAgentStateStore implementation");
        }
    }
}
