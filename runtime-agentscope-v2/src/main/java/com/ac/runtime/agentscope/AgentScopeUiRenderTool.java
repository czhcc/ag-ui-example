package com.ac.runtime.agentscope;

import com.ac.richui.core.context.AccessSubject;
import com.ac.richui.core.presentation.PresentationService;
import com.ac.richui.core.result.ResultReference;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.ToolCallParam;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import reactor.core.publisher.Mono;

/** Local presentation tool. Its SurfaceSpec leaves AgentScope as a ui.surface.create CustomEvent. */
public final class AgentScopeUiRenderTool implements AgentTool {
    private static final Map<String, Object> PARAMETERS = Map.of(
            "type", "object",
            "properties", Map.of(
                    "resultRef", Map.of("type", "string", "description", "resultRef from an MCP observation"),
                    "viewId", Map.of("type", "string", "description", "viewId recommended by that result")),
            "required", List.of("resultRef", "viewId"),
            "additionalProperties", false);

    private final PresentationService presentations;

    public AgentScopeUiRenderTool(PresentationService presentations) {
        this.presentations = Objects.requireNonNull(presentations, "presentations must not be null");
    }

    @Override public String getName() { return "ui_render"; }
    @Override public String getDescription() { return "Render one validated view from a previous MCP result"; }
    @Override public Map<String, Object> getParameters() { return PARAMETERS; }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        return Mono.fromCallable(() -> {
            String toolCallId = Objects.requireNonNull(param.getToolUseBlock(), "toolUseBlock is required").getId();
            AgentScopeRunContext run = AgentScopeRuntimeContexts.require(param.getRuntimeContext());
            var scope = run.scope().withToolCallId(toolCallId);
            String resultRef = required(param.getInput(), "resultRef");
            String viewId = required(param.getInput(), "viewId");
            var subject = AccessSubject.of(scope.tenantId(), scope.userId());
            var surface = presentations.render(scope, subject, new ResultReference(resultRef), viewId);
            var payload = new java.util.LinkedHashMap<String, Object>();
            payload.put("profile", surface.profile());
            payload.put("profileVersion", surface.profileVersion());
            payload.put("surfaceId", surface.surfaceId());
            payload.put("dataRef", surface.dataRef());
            payload.put("components", surface.components());
            if (surface.revision() > 0) payload.put("revision", surface.revision());
            run.emit("ui.surface.create", payload);
            return ToolResultBlock.text(
                    "The validated UI surface was added to this run. surfaceId=" + surface.surfaceId()
                            + ". Explain only the key conclusion.");
        });
    }

    private String required(Map<String, Object> input, String key) {
        Object value = input.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(key + " must be a non-blank string");
        }
        return text;
    }
}
