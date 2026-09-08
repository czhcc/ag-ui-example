package com.ac.agent.mcp.config;

import java.time.Duration;

public record McpServerDefinition(String code, String name, String baseUrl, String endpoint,
                                  Duration timeout, boolean enabled, McpAuthConfig auth, String toolPrefix) {
    public McpServerDefinition {
        endpoint = endpoint == null || endpoint.isBlank() ? "/mcp" : endpoint;
        timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
        auth = auth == null ? new McpAuthConfig(null, null, null, null) : auth;
        toolPrefix = toolPrefix == null ? "" : toolPrefix;
    }
}
