package com.ac.agent.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties("ac.mcp")
public record McpServerProperties(List<McpServerDefinition> servers) {
    public McpServerProperties { servers = servers == null ? List.of() : List.copyOf(servers); }
}
