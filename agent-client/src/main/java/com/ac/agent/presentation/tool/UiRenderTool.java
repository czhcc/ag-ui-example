package com.ac.agent.presentation.tool;

import com.ac.richui.core.context.AccessSubject;
import com.ac.richui.core.event.RuntimeEventSink;
import com.ac.richui.core.event.SurfaceCreated;
import com.ac.richui.core.presentation.PresentationService;
import com.ac.richui.core.result.ResultReference;
import com.ac.runtime.saa.SaaRunMetadata;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public final class UiRenderTool {
    private final PresentationService presentations;
    private final RuntimeEventSink events;

    public UiRenderTool(PresentationService presentations, RuntimeEventSink events) {
        this.presentations = presentations;
        this.events = events;
    }

    @Tool(name = "ui_render", description = "Render one validated view from a previous MCP result")
    public String render(
            @ToolParam(description = "resultRef from the MCP observation") String resultRef,
            @ToolParam(description = "viewId recommended by the MCP result") String viewId,
            ToolContext toolContext) {
        var scope = SaaRunMetadata.requireScope(toolContext.getContext());
        var subject = AccessSubject.of(scope.tenantId(), scope.userId());
        var surface = presentations.render(scope, subject, new ResultReference(resultRef), viewId);
        events.publish(scope, new SurfaceCreated(surface));
        return "The validated UI surface was added to this run. Explain only the key conclusion.";
    }
}
