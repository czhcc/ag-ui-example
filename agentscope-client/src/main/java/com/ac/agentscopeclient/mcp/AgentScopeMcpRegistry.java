package com.ac.agentscopeclient.mcp;

import com.ac.agentscopeclient.config.AgentScopeMcpProperties;
import com.ac.runtime.agentscope.AgentScopeMcpTool;
import com.ac.richui.core.tool.RichToolResultProcessor;
import com.ac.richui.mcp.McpCallToolResultAdapter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Uses AgentScope's MCP client while decorating discovered tools with the shared result pipeline. */
@Component
public final class AgentScopeMcpRegistry {
    private final Map<String, McpClientWrapper> clients = new LinkedHashMap<>();
    private final List<AgentTool> tools;

    public AgentScopeMcpRegistry(
            AgentScopeMcpProperties properties,
            McpCallToolResultAdapter results,
            RichToolResultProcessor processor,
            ObjectMapper mapper) {
        List<AgentTool> discovered = new ArrayList<>();
        Map<String, String> owners = new LinkedHashMap<>();
        for (AgentScopeMcpProperties.Server server : properties.servers()) {
            if (!server.enabled()) continue;
            McpClientBuilder builder = McpClientBuilder.create(server.code())
                    .streamableHttpTransport(server.url())
                    .timeout(server.timeout());
            switch (server.auth().type()) {
                case BEARER -> builder.header("Authorization", "Bearer " + server.auth().token());
                case API_KEY, CUSTOM_HEADER -> builder.header(
                        required(server.auth().headerName(), "MCP auth headerName"),
                        required(server.auth().headerValue(), "MCP auth headerValue"));
                case NONE -> { }
            }
            McpClientWrapper client = builder.buildSync();
            client.initialize().block(server.timeout());
            clients.put(server.code(), client);
            var remoteTools = client.listTools().block(server.timeout());
            if (remoteTools == null) continue;
            for (var tool : remoteTools) {
                String exposedName = server.toolPrefix() + tool.name();
                String previous = owners.putIfAbsent(exposedName, server.code());
                if (previous != null) {
                    throw new IllegalStateException("MCP tool name collision: " + exposedName
                            + " (" + previous + ", " + server.code() + ")");
                }
                Map<String, Object> schema = mapper.convertValue(
                        tool.inputSchema(), new TypeReference<>() { });
                discovered.add(new AgentScopeMcpTool(
                        exposedName, tool.description(), schema, server.code(), tool.name(),
                        client::callTool, results, processor));
            }
        }
        this.tools = List.copyOf(discovered);
    }

    public List<AgentTool> tools() { return tools; }

    @PreDestroy
    public void close() {
        clients.values().forEach(McpClientWrapper::close);
        clients.clear();
    }

    private String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
