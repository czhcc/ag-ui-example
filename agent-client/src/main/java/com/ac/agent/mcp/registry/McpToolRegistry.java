package com.ac.agent.mcp.registry;

import com.ac.agent.mcp.client.McpClientManager;
import com.ac.agent.mcp.tool.McpToolCallbackFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class McpToolRegistry {
    private static final Logger log = LoggerFactory.getLogger(McpToolRegistry.class);
    private final Map<String, ToolCallback> callbacks = new ConcurrentHashMap<>();
    private final Map<String, String> owners = new ConcurrentHashMap<>();
    private final McpClientManager clients;
    private final McpServerRegistry servers;
    private final McpToolCallbackFactory factory;
    private final com.ac.mcpapps.core.McpAppCatalog apps;
    private final com.fasterxml.jackson.databind.ObjectMapper mapper;
    public McpToolRegistry(McpClientManager clients, McpServerRegistry servers, McpToolCallbackFactory factory,
                          com.ac.mcpapps.core.McpAppCatalog apps, com.fasterxml.jackson.databind.ObjectMapper mapper) {
        this.clients = clients; this.servers = servers; this.factory = factory;
        this.apps = apps; this.mapper = mapper;
    }
    public synchronized void discover(String serverCode) {
        var server = servers.required(serverCode);
        removeServer(serverCode);
        var tools = clients.getClient(serverCode).listTools().tools();
        apps.transport(serverCode, new com.ac.mcpapps.core.McpAppTransport() {
            public Map<String, Object> readResource(String uri) {
                return mapper.convertValue(clients.getClient(serverCode).readResource(
                        new io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest(uri)), Map.class);
            }
            public Map<String, Object> callTool(String name, Map<String, Object> arguments, com.ac.richui.core.context.AccessSubject subject) {
                return mapper.convertValue(clients.getClient(serverCode).callTool(
                        io.modelcontextprotocol.spec.McpSchema.CallToolRequest.builder().name(name).arguments(arguments)
                                .meta(Map.of("ac.subject", Map.of("tenantId", subject.tenantId(), "userId", subject.userId()))).build()), Map.class);
            }
        });
        tools.forEach(tool -> {
            var appTool = apps.register(serverCode, tool.name(), tool.title(), mapper.convertValue(tool.inputSchema(), Map.class),
                    tool.meta(), tool.annotations() != null && Boolean.TRUE.equals(tool.annotations().readOnlyHint()));
            if (!appTool.modelVisible()) return;
            var callback = factory.create(serverCode, server.toolPrefix(), tool);
            var name = callback.getToolDefinition().name();
            if (callbacks.putIfAbsent(name, callback) != null) throw new IllegalStateException("MCP tool name collision: " + name);
            owners.put(name, serverCode);
        });
        log.info("Discovered MCP tools serverCode={} count={}", serverCode, tools.size());
    }
    public Collection<ToolCallback> callbacks() { return List.copyOf(callbacks.values()); }
    public synchronized void removeServer(String serverCode) {
        apps.remove(serverCode);
        var names = owners.entrySet().stream().filter(entry -> entry.getValue().equals(serverCode)).map(Map.Entry::getKey).toList();
        names.forEach(name -> { callbacks.remove(name); owners.remove(name); });
    }
}
