package com.ac.agent.mcp.tool;

import com.ac.agent.mcp.client.McpClientManager;
import com.ac.richui.core.event.RuntimeEventSink;
import com.ac.richui.core.tool.RichToolResultProcessor;
import com.ac.richui.mcp.McpCallToolResultAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

@Component
public class McpToolCallbackFactory {
    private final McpToolDefinitionAdapter definitions;
    private final McpClientManager clients;
    private final McpCallToolResultAdapter results;
    private final RichToolResultProcessor processor;
    private final RuntimeEventSink events;
    private final ObjectMapper mapper;

    public McpToolCallbackFactory(
            McpToolDefinitionAdapter definitions,
            McpClientManager clients,
            McpCallToolResultAdapter results,
            RichToolResultProcessor processor,
            RuntimeEventSink events,
            ObjectMapper mapper) {
        this.definitions = definitions;
        this.clients = clients;
        this.results = results;
        this.processor = processor;
        this.events = events;
        this.mapper = mapper;
    }

    public ToolCallback create(String serverCode, String prefix, McpSchema.Tool tool) {
        String exposedName = prefix + tool.name();
        return new DynamicMcpToolCallback(
                serverCode,
                tool.name(),
                definitions.adapt(exposedName, tool),
                clients,
                results,
                processor,
                events,
                mapper);
    }
}
