package com.ac.runtime.agentscope;

import com.ac.richui.core.tool.RichToolResultProcessor;
import com.ac.richui.core.tool.ToolIdentity;
import com.ac.richui.mcp.McpCallToolResultAdapter;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.ToolCallParam;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import reactor.core.publisher.Mono;

/** AgentTool decorator that routes every MCP result through the shared rich-result pipeline. */
public final class AgentScopeMcpTool implements AgentTool {
    @FunctionalInterface
    public interface Invoker {
        Mono<McpSchema.CallToolResult> call(String toolName, Map<String, Object> arguments);
    }

    private final String exposedName;
    private final String description;
    private final Map<String, Object> parameters;
    private final String serverCode;
    private final String remoteToolName;
    private final Invoker invoker;
    private final McpCallToolResultAdapter resultAdapter;
    private final RichToolResultProcessor processor;

    public AgentScopeMcpTool(
            String exposedName, String description, Map<String, Object> parameters,
            String serverCode, String remoteToolName, Invoker invoker,
            McpCallToolResultAdapter resultAdapter, RichToolResultProcessor processor) {
        this.exposedName = requireText(exposedName, "exposedName");
        this.description = description == null || description.isBlank() ? "MCP tool " + remoteToolName : description;
        this.parameters = parameters == null ? Map.of("type", "object") : Map.copyOf(parameters);
        this.serverCode = requireText(serverCode, "serverCode");
        this.remoteToolName = requireText(remoteToolName, "remoteToolName");
        this.invoker = Objects.requireNonNull(invoker, "invoker must not be null");
        this.resultAdapter = Objects.requireNonNull(resultAdapter, "resultAdapter must not be null");
        this.processor = Objects.requireNonNull(processor, "processor must not be null");
    }

    @Override public String getName() { return exposedName; }
    @Override public String getDescription() { return description; }
    @Override public Map<String, Object> getParameters() { return parameters; }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        Objects.requireNonNull(param, "param must not be null");
        String toolCallId = param.getToolUseBlock() == null ? null : param.getToolUseBlock().getId();
        if (toolCallId == null || toolCallId.isBlank()) {
            return Mono.just(ToolResultBlock.error("AgentScope toolCallId is required"));
        }
        AgentScopeRunContext run = AgentScopeRuntimeContexts.require(param.getRuntimeContext());
        var scope = run.scope().withToolCallId(toolCallId);
        return invoker.call(remoteToolName, param.getInput())
                .map(resultAdapter::adapt)
                .map(raw -> processor.process(scope, new ToolIdentity(serverCode, remoteToolName), raw))
                .map(processed -> {
                    Map<String, Object> event = new LinkedHashMap<>();
                    event.put("toolCallId", toolCallId);
                    event.put("toolName", exposedName);
                    event.put("kind", processed.kind().name());
                    event.put("summary", processed.publicSummary());
                    if (processed.resultReference() != null) {
                        event.put("resultRef", processed.resultReference().value());
                    }
                    run.emit("ac.rich-ui.tool-result", Map.copyOf(event));
                    return ToolResultBlock.text(processed.observation().content());
                })
                .onErrorResume(error -> Mono.just(ToolResultBlock.error("MCP tool execution failed")));
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
