package com.ac.mcp.server.tool.activity;

import com.ac.mcp.server.service.ActivityService;
import com.ac.mcp.server.tool.McpResults;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class ActivityListTool {
    private final ActivityService service;
    public ActivityListTool(ActivityService service) { this.service = service; }

    @McpTool(name = "kg_list_activity", description = "查询指定人员在时间范围内的活动")
    public Map<String, Object> list(String personId, Instant startTime, Instant endTime) {
        var data = service.list(personId, startTime, endTime);
        return McpResults.success(data,
                McpResults.summary(data.size(), data.size(), false, "活动列表", List.of()),
                McpResults.recommended(McpResults.view("activity-timeline", "timeline", "vertical", "活动时间线", null,
                        Map.of("time", "occurredAt", "title", "city"), Map.of(), 5)));
    }
}
