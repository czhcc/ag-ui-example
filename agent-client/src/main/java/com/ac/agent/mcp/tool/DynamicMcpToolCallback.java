package com.ac.agent.mcp.tool;

import com.ac.agent.mcp.client.McpClientManager;
import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.event.RuntimeEventSink;
import com.ac.richui.core.event.StandardRuntimeEvent;
import com.ac.richui.core.tool.RichToolResultProcessor;
import com.ac.richui.core.tool.ToolIdentity;
import com.ac.richui.mcp.McpCallToolResultAdapter;
import com.ac.runtime.saa.SaaRunMetadata;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/** MCP callback decorated with the shared rich-result processing pipeline. */
public final class DynamicMcpToolCallback implements ToolCallback {
    private final String serverCode;
    private final String remoteToolName;
    private final ToolDefinition definition;
    private final McpClientManager clients;
    private final McpCallToolResultAdapter adapter;
    private final RichToolResultProcessor processor;
    private final RuntimeEventSink events;
    private final ObjectMapper mapper;

    public DynamicMcpToolCallback(
            String serverCode,
            String remoteToolName,
            ToolDefinition definition,
            McpClientManager clients,
            McpCallToolResultAdapter adapter,
            RichToolResultProcessor processor,
            RuntimeEventSink events,
            ObjectMapper mapper) {
        this.serverCode = serverCode;
        this.remoteToolName = remoteToolName;
        this.definition = definition;
        this.clients = clients;
        this.adapter = adapter;
        this.processor = processor;
        this.events = events;
        this.mapper = mapper;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return definition;
    }

    @Override
    public String call(String toolInput) {
        throw new IllegalStateException("MCP tools require a trusted SAA ToolContext");
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        try {
            RunScope scope = SaaRunMetadata.requireScope(toolContext.getContext());
            Map<String, Object> arguments = mapper.readValue(toolInput, new TypeReference<>() { });
            McpSchema.CallToolRequest request = McpSchema.CallToolRequest.builder()
                    .name(remoteToolName)
                    .arguments(arguments)
                    .build();
            var nativeResult = clients.getClient(serverCode).callTool(request);
            var processed = processor.process(
                    scope,
                    new ToolIdentity(serverCode, remoteToolName),
                    adapter.adapt(nativeResult));

            Map<String, Object> attributes = new LinkedHashMap<>();
            attributes.put("toolCallId", scope.toolCallId());
            attributes.put("toolName", definition.name());
            attributes.put("kind", processed.kind().name());
            attributes.put("summary", processed.publicSummary());
            if (processed.resultReference() != null) {
                attributes.put("resultRef", processed.resultReference().value());
            }
            events.publish(scope, new StandardRuntimeEvent("tool.result", attributes));
            return processed.observation().content();
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("MCP tool call failed: " + definition.name(), exception);
        }
    }
}
