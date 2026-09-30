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
public class ActivityHeatmapTool {
    private final PhaseTwoFixtureService service;
    public ActivityHeatmapTool(PhaseTwoFixtureService service) { this.service = service; }

    @McpTool(name = "kg_activity_heatmap", description = "查询城市与活动类型并返回推荐可视化视图")
    public Map<String, Object> query(@ToolParam(description = "人员ID") String personId) {
        if (PhaseTwoResults.invalid(personId)) return McpResults.failureV12("INVALID_ARGUMENT", "Required parameter is missing or too long");

        var data = service.heatmap(personId);
        var view = McpResults.view("activity-heatmap", "heatmap", "matrix", "城市与活动类型", null,
                Map.of("x", "city", "y", "eventType", "value", "count"), Map.of("limit", 1000, "showLabels", true), 10);
        return PhaseTwoResults.result(data, "城市与活动类型", data.size(), view, Map.of("personId", personId));
    }
}
