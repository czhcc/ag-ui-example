package com.ac.mcp.server.tool.activity;

import com.ac.mcp.contract.presentation.PresentationHint;
import com.ac.mcp.contract.presentation.TimelineViewHint;
import com.ac.mcp.contract.result.*;
import com.ac.mcp.server.domain.Activity;
import com.ac.mcp.server.service.ActivityService;
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
    public McpResult<List<Activity>> list(String personId, Instant startTime, Instant endTime) {
        var data = service.list(personId, startTime, endTime);
        var view = new TimelineViewHint("activity-timeline", "timeline", "vertical", "活动时间线", null,
                Map.of("time", "occurredAt", "title", "city"), Map.of(), 5);
        return McpResult.success(data, new ResultSummary(data.size(), data.size(), false, "活动列表", List.of()),
                PresentationHint.recommended(view), null);
    }
}
