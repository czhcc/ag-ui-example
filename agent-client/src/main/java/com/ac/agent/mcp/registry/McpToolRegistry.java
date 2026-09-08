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
    public McpToolRegistry(McpClientManager clients, McpServerRegistry servers, McpToolCallbackFactory factory) {
        this.clients = clients; this.servers = servers; this.factory = factory;
    }
    public synchronized void discover(String serverCode) {
        var server = servers.required(serverCode);
        removeServer(serverCode);
        var tools = clients.getClient(serverCode).listTools().tools();
        tools.forEach(tool -> {
            var callback = factory.create(serverCode, server.toolPrefix(), tool);
            var name = callback.getToolDefinition().name();
            if (callbacks.putIfAbsent(name, callback) != null) throw new IllegalStateException("MCP tool name collision: " + name);
            owners.put(name, serverCode);
        });
        log.info("Discovered MCP tools serverCode={} count={}", serverCode, tools.size());
    }
    public Collection<ToolCallback> callbacks() { return List.copyOf(callbacks.values()); }
    public synchronized void removeServer(String serverCode) {
        var names = owners.entrySet().stream().filter(entry -> entry.getValue().equals(serverCode)).map(Map.Entry::getKey).toList();
        names.forEach(name -> { callbacks.remove(name); owners.remove(name); });
    }
}
