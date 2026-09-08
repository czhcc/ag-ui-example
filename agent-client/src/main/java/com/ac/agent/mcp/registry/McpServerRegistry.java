package com.ac.agent.mcp.registry;

import com.ac.agent.mcp.config.*;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class McpServerRegistry {
    private final Map<String, McpServerDefinition> definitions;
    public McpServerRegistry(McpServerProperties properties) {
        this.definitions = properties.servers().stream().collect(Collectors.toUnmodifiableMap(McpServerDefinition::code, Function.identity()));
    }
    public McpServerDefinition required(String code) {
        var definition = definitions.get(code);
        if (definition == null || !definition.enabled()) throw new IllegalArgumentException("Unknown or disabled MCP server: " + code);
        return definition;
    }
    public Map<String, McpServerDefinition> all() { return definitions; }
}
