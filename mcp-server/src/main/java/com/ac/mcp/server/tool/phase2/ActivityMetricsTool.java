package com.ac.mcp.server.tool.phase2;

import com.ac.mcp.server.service.PhaseTwoFixtureService;
import com.ac.mcp.server.tool.McpResults;
import com.ac.mcp.server.tool.PhaseTwoResults;
import java.util.Map;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"demo", "test"})
public class ActivityMetricsTool {
    private final PhaseTwoFixtureService service;
    public ActivityMetricsTool(PhaseTwoFixtureService service) { this.service = service; }

    @McpTool(name = "kg_activity_metrics", description = "查询活动概览并返回推荐可视化视图")
    public Map<String, Object> query(@ToolParam(description = "人员ID") String personId) {
        if (PhaseTwoResults.invalid(personId)) return McpResults.failureV12("INVALID_ARGUMENT", "Required parameter is missing or too long");

        var data = service.metrics(personId);
        var view = McpResults.view("activity-metrics", "metric", "single", "活动概览", null,
                Map.of("label", "label", "value", "value", "unit", "unit"), Map.of("limit", 12), 10);
        return PhaseTwoResults.result(data, "活动概览", data.size(), view, Map.of("personId", personId));
    }
}
