package com.ac.agent.mcp.tool;

import com.ac.agent.mcp.client.McpClientManager;
import com.ac.agent.mcp.observation.ObservationBuilder;
import com.ac.agent.mcp.result.ResultStore;
import com.ac.agent.streaming.AgentEventBus;
import com.ac.agent.streaming.event.ToolEndEvent;
import com.ac.agent.streaming.event.ToolStartEvent;
import com.ac.mcp.contract.result.McpBusinessError;
import com.ac.mcp.contract.result.McpResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import java.util.Map;
import java.util.UUID;

public class DynamicMcpToolCallback implements ToolCallback {
    private static final Logger log = LoggerFactory.getLogger(DynamicMcpToolCallback.class);
    private final String serverCode;
    private final String remoteToolName;
    private final ToolDefinition definition;
    private final McpClientManager clients;
    private final ResultStore resultStore;
    private final ObservationBuilder observations;
    private final AgentEventBus events;
    private final ObjectMapper mapper;

    public DynamicMcpToolCallback(String serverCode, String remoteToolName, ToolDefinition definition,
                                  McpClientManager clients, ResultStore resultStore, ObservationBuilder observations,
                                  AgentEventBus events, ObjectMapper mapper) {
        this.serverCode = serverCode; this.remoteToolName = remoteToolName; this.definition = definition;
        this.clients = clients; this.resultStore = resultStore; this.observations = observations; this.events = events; this.mapper = mapper;
    }
    @Override public ToolDefinition getToolDefinition() { return definition; }

    private static final int MAX_RESULT_JSON_CHARS = 20_000;

    @Override public String call(String toolInput) {
        var callId = UUID.randomUUID().toString();
        var started = System.nanoTime();
        log.info("MCP tool call started serverCode={} tool={}", serverCode, remoteToolName);
        try {
            Map<String, Object> arguments = mapper.readValue(toolInput, new TypeReference<>() { });
            events.emit(new ToolStartEvent(callId, definition.name(), toolInput));
            var request = McpSchema.CallToolRequest.builder().name(remoteToolName).arguments(arguments).build();
            var callResult = clients.getClient(serverCode).callTool(request);
            McpResult<?> result = parse(callResult);
            String ref = resultStore.save(serverCode, remoteToolName, result);
            events.emit(new ToolEndEvent(callId, definition.name(), result.success(), ref,
                    truncate(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result)), null));
            log.info("MCP tool call completed serverCode={} tool={} elapsedMs={}", serverCode, remoteToolName, elapsedMillis(started));
            return observations.build(definition.name(), ref, result);
        } catch (Exception exception) {
            events.emit(new ToolEndEvent(callId, definition.name(), false, null, null,
                    exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage()));
            log.warn("MCP tool call failed serverCode={} tool={} elapsedMs={} reason={}", serverCode, remoteToolName, elapsedMillis(started), exception.getMessage());
            throw new IllegalStateException("MCP tool call failed: " + definition.name(), exception);
        }
    }

    private McpResult<?> parse(McpSchema.CallToolResult callResult) throws Exception {
        if (Boolean.TRUE.equals(callResult.isError())) {
            return McpResult.failure(new McpBusinessError("MCP_PROTOCOL_ERROR", firstText(callResult), true), null);
        }
        if (callResult.structuredContent() != null) {
            return mapper.convertValue(callResult.structuredContent(), new TypeReference<McpResult<Object>>() { });
        }
        String text = firstText(callResult);
        if (text == null || text.isBlank()) throw new IllegalStateException("MCP result contains neither structuredContent nor text JSON");
        return mapper.readValue(text, new TypeReference<McpResult<Object>>() { });
    }

    private String firstText(McpSchema.CallToolResult result) {
        return result.content().stream().filter(McpSchema.TextContent.class::isInstance)
                .map(McpSchema.TextContent.class::cast).map(McpSchema.TextContent::text).findFirst().orElse(null);
    }
    private long elapsedMillis(long started) { return (System.nanoTime() - started) / 1_000_000; }

    private String truncate(String json) {
        if (json == null || json.length() <= MAX_RESULT_JSON_CHARS) return json;
        return json.substring(0, MAX_RESULT_JSON_CHARS) + "\n…(已截断，完整结果共 " + json.length() + " 字符)";
    }
}
