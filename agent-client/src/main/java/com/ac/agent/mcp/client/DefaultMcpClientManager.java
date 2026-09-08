package com.ac.agent.mcp.client;

import com.ac.agent.mcp.registry.McpServerRegistry;
import io.modelcontextprotocol.client.McpSyncClient;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DefaultMcpClientManager implements McpClientManager {
    private static final Logger log = LoggerFactory.getLogger(DefaultMcpClientManager.class);
    private final ConcurrentHashMap<String, McpSyncClient> clients = new ConcurrentHashMap<>();
    private final McpClientFactory factory;
    private final McpServerRegistry registry;
    public DefaultMcpClientManager(McpClientFactory factory, McpServerRegistry registry) { this.factory = factory; this.registry = registry; }
    @Override public McpSyncClient getClient(String code) { return clients.computeIfAbsent(code, key -> factory.create(registry.required(key))); }
    @Override public void refresh(String code) { remove(code); getClient(code); log.info("Refreshed MCP connection serverCode={}", code); }
    @Override public void remove(String code) { var client = clients.remove(code); if (client != null) client.closeGracefully(); }
    @Override @PreDestroy public void closeAll() { clients.forEach((code, client) -> client.closeGracefully()); clients.clear(); }
}
