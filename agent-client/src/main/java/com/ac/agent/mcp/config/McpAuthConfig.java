package com.ac.agent.mcp.config;

public record McpAuthConfig(AuthType type, String token, String headerName, String headerValue) {
    public McpAuthConfig { type = type == null ? AuthType.NONE : type; }
    public enum AuthType { NONE, BEARER, API_KEY, CUSTOM_HEADER }
}
