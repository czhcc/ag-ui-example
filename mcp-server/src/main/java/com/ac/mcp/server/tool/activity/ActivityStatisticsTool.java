package com.ac.mcp.server.tool.activity;

import com.ac.mcp.server.service.ActivityService;
import com.ac.mcp.server.tool.McpResults;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.Map;

@Component
public class ActivityStatisticsTool {
    private final ActivityService service;
    public ActivityStatisticsTool(ActivityService service) { this.service = service; }

    @McpTool(name = "kg_activity_statistics", description = "统计指定人员在时间范围内的活动情况，按城市汇总")
    public Map<String, Object> statistics(
            @ToolParam(description = "人员ID，如 person-001") String personId,
            @ToolParam(description = "开始时间") Instant startTime,
            @ToolParam(description = "结束时间") Instant endTime) {
        var data = service.statistics(personId, startTime, endTime);
        return McpResults.success(data,
                McpResults.summary(data.size(), data.size(), false, "按城市统计活动次数",
                        data.stream().map(it -> it.city() + it.count() + "次").toList()),
                McpResults.recommended(McpResults.view("city-stat", "chart", "bar", "各城市活动次数", "按城市汇总",
                        Map.of("category", "city", "value", "count"), Map.of(), 10,
                        McpResults.drillDown("city", "深入分析该城市",
                                "深入分析 {personId} 在 {city} 的活动情况，展示活动明细时间线"))),
                Map.of("personId", personId,
                        "startTime", startTime == null ? "" : startTime.toString(),
                        "endTime", endTime == null ? "" : endTime.toString()));
    }
}
