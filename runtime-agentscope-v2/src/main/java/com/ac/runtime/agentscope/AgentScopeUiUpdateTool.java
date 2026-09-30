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

/** Updates an existing surface within the creating run. */
public final class AgentScopeUiUpdateTool implements AgentTool {
    private static final Map<String, Object> PARAMETERS = Map.of(
            "type", "object",
            "properties", Map.of(
                    "surfaceId", Map.of("type", "string"),
                    "resultRef", Map.of("type", "string"),
                    "viewId", Map.of("type", "string")),
            "required", List.of("surfaceId", "resultRef", "viewId"),
            "additionalProperties", false);
    private final PresentationService presentations;

    public AgentScopeUiUpdateTool(PresentationService presentations) {
        this.presentations = Objects.requireNonNull(presentations);
    }
    @Override public String getName() { return "ui_update"; }
    @Override public String getDescription() { return "Update one surface in this run using another validated MCP result"; }
    @Override public Map<String, Object> getParameters() { return PARAMETERS; }
    @Override public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        return Mono.fromCallable(() -> {
            String toolCallId = Objects.requireNonNull(param.getToolUseBlock()).getId();
            AgentScopeRunContext run = AgentScopeRuntimeContexts.require(param.getRuntimeContext());
            var scope = run.scope().withToolCallId(toolCallId);
            var subject = AccessSubject.of(scope.tenantId(), scope.userId());
            var surface = presentations.update(scope, subject, required(param, "surfaceId"),
                    new ResultReference(required(param, "resultRef")), required(param, "viewId"));
            run.emit("ui.surface.update", Map.of(
                    "profile", surface.profile(), "profileVersion", surface.profileVersion(),
                    "surfaceId", surface.surfaceId(), "dataRef", surface.dataRef(),
                    "components", surface.components(), "revision", surface.revision()));
            return ToolResultBlock.text("The validated UI surface was updated in this run.");
        });
    }
    private String required(ToolCallParam param, String key) {
        Object value = param.getInput().get(key);
        if (!(value instanceof String text) || text.isBlank())
            throw new IllegalArgumentException(key + " must be a nonblank string");
        return text;
    }
}
