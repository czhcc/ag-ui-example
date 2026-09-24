package com.ac.agentscopeclient.config;

import io.agentscope.core.state.AgentStateStore;

/** Marker for an AgentScope state store shared by every application replica. */
public interface SharedAgentStateStore extends AgentStateStore { }
