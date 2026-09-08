package com.ac.agent.mcp.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.stereotype.Component;

@Component
public class McpToolDefinitionAdapter {
    private final ObjectMapper mapper;
    public McpToolDefinitionAdapter(ObjectMapper mapper) { this.mapper = mapper; }
    public ToolDefinition adapt(String exposedName, McpSchema.Tool tool) {
        try {
            return ToolDefinition.builder().name(exposedName)
                    .description(tool.description() == null ? "MCP tool " + tool.name() : tool.description())
                    .inputSchema(mapper.writeValueAsString(tool.inputSchema())).build();
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid input schema for MCP tool " + tool.name(), exception);
        }
    }
}
