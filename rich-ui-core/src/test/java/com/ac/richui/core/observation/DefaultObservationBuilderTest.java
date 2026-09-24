package com.ac.richui.core.observation;

import com.ac.mcp.contract.presentation.ChartMapping;
import com.ac.mcp.contract.presentation.ChartViewHint;
import com.ac.mcp.contract.presentation.PresentationHint;
import com.ac.mcp.contract.result.McpResult;
import com.ac.mcp.contract.result.ResultSummary;
import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.result.ResultReference;
import com.ac.richui.core.text.SensitiveTextSanitizer;
import com.ac.richui.core.tool.ToolIdentity;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultObservationBuilderTest {
    @Test
    void redactsAndBoundsSummaryWithoutCopyingData() {
        var policy = new ObservationPolicy(180, 2, 40, 40, 1);
        var builder = new DefaultObservationBuilder(policy, new SensitiveTextSanitizer());
        var result = McpResult.success(
                Map.of("privatePayload", "must-not-enter-observation"),
                new ResultSummary(3, 3, false,
                        "password=hunter2 联系 alice@example.com",
                        List.of("第一条", "第二条", "第三条")),
                PresentationHint.none(),
                null);

        String observation = builder.build(
                new RunScope("t", "u", "thread", "run", "call"),
                new ToolIdentity("server", "tool"),
                new ResultReference("result_1"),
                result).content();

        assertTrue(observation.length() <= 180);
        assertTrue(observation.contains("[REDACTED]"));
        assertFalse(observation.contains("hunter2"));
        assertFalse(observation.contains("alice@example.com"));
        assertFalse(observation.contains("must-not-enter-observation"));
        assertFalse(observation.contains("第三条"));
    }

    @Test
    void requiresUiRenderWhenTheToolProvidesViews() {
        var builder = new DefaultObservationBuilder();
        var result = McpResult.success(
                Map.of("city", "北京", "count", 8),
                new ResultSummary(1, 1, false, "按城市统计活动次数", List.of("北京8次")),
                PresentationHint.recommended(new ChartViewHint(
                        "city-stat", "chart", "bar", "各城市活动次数", null,
                        new ChartMapping("city", "count"), Map.of(), 0, null)),
                null);

        String observation = builder.build(
                new RunScope("t", "u", "thread", "run", "call"),
                new ToolIdentity("knowledge", "kg_activity_statistics"),
                new ResultReference("result_1"),
                result).content();

        assertTrue(observation.contains("必须调用一次 ui_render"));
        assertTrue(observation.contains("viewId=city-stat"));
        assertFalse(observation.contains("文字已足够时不要调用"));
    }
}
