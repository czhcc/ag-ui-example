package com.ac.mcp.server.tool.activity;

import com.ac.mcp.server.domain.ActivityStat;
import com.ac.mcp.server.service.ActivityService;
import com.ac.mcp.server.tool.McpResults;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class ActivityStatisticsTool {
    private final ActivityService service;
    public ActivityStatisticsTool(ActivityService service) { this.service = service; }

    @McpTool(name = "kg_activity_statistics", description = "统计指定人员在时间范围内的活动情况")
    public Map<String, Object> statistics(String personId, Instant startTime, Instant endTime) {
        var data = service.statistics(personId, startTime, endTime);
        return McpResults.success(data,
                McpResults.summary(data.size(), data.size(), false, "按城市统计活动次数",
                        data.stream().map(it -> it.city() + it.count() + "次").toList()),
                McpResults.recommended(McpResults.view("city-stat", "chart", "bar", "各城市活动次数", "按城市汇总",
                        Map.of("category", "city", "value", "count"), Map.of(), 10)));
    }
}
