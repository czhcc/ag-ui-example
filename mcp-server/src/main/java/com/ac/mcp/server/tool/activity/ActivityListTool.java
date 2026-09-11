package com.ac.mcp.server.tool.activity;

import com.ac.mcp.server.service.ActivityService;
import com.ac.mcp.server.tool.McpResults;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class ActivityListTool {
    private final ActivityService service;
    public ActivityListTool(ActivityService service) { this.service = service; }

    @McpTool(name = "kg_list_activity", description = "查询指定人员在时间范围内的活动明细，可按城市过滤")
    public Map<String, Object> list(
            @ToolParam(description = "人员ID，如 person-001") String personId,
            @ToolParam(description = "开始时间") Instant startTime,
            @ToolParam(description = "结束时间") Instant endTime,
            @ToolParam(required = false, description = "城市过滤，如 北京；不传则返回所有城市") String city) {
        var data = service.list(personId, startTime, endTime, city);
        return McpResults.success(data,
                McpResults.summary(data.size(), data.size(), false,
                        (city == null || city.isBlank() ? "活动列表" : city + "的活动明细"), List.of()),
                McpResults.recommended(McpResults.view("activity-timeline", "timeline", "vertical", "活动时间线", null,
                        Map.of("time", "occurredAt", "title", "description", "group", "eventType"), Map.of(), 5)));
    }
}
