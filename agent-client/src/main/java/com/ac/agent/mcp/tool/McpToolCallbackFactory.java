package com.ac.agent.mcp.tool;

import com.ac.agent.mcp.client.McpClientManager;
import com.ac.agent.mcp.observation.ObservationBuilder;
import com.ac.agent.mcp.result.ResultStore;
import com.ac.agent.streaming.AgentEventBus;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

@Component
public class McpToolCallbackFactory {
    private final McpToolDefinitionAdapter adapter;
    private final McpClientManager clients;
    private final ResultStore store;
    private final ObservationBuilder observations;
    private final AgentEventBus events;
    private final ObjectMapper mapper;
    public McpToolCallbackFactory(McpToolDefinitionAdapter adapter, McpClientManager clients, ResultStore store,
                                  ObservationBuilder observations, AgentEventBus events, ObjectMapper mapper) {
        this.adapter = adapter; this.clients = clients; this.store = store; this.observations = observations; this.events = events; this.mapper = mapper;
    }
    public ToolCallback create(String serverCode, String prefix, McpSchema.Tool tool) {
        String exposedName = prefix + tool.name();
        return new DynamicMcpToolCallback(serverCode, tool.name(), adapter.adapt(exposedName, tool), clients, store, observations, events, mapper);
    }
}
