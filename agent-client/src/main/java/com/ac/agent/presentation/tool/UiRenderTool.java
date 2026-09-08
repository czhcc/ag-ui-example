package com.ac.agent.presentation.tool;

import com.ac.agent.presentation.runtime.PresentationRuntime;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class UiRenderTool {
    private final PresentationRuntime runtime;
    public UiRenderTool(PresentationRuntime runtime) { this.runtime = runtime; }
    @Tool(name = "ui_render", description = "根据MCP结果中的ViewHint插入可视化组件")
    public String render(@ToolParam(description = "MCP observation中的resultRef") String resultRef,
                         @ToolParam(description = "MCP推荐展示的viewId") String viewId) {
        runtime.render(resultRef, viewId);
        return "UI 已插入当前回复。不要重复逐条输出图中的全部数据，请继续解释关键结论。";
    }
}
