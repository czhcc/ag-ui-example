package com.ac.mcp.server.tool.relation;

import com.ac.mcp.server.service.RelationService;
import com.ac.mcp.server.tool.McpResults;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class RelationStatisticsTool {
    private final RelationService service;
    public RelationStatisticsTool(RelationService service) { this.service = service; }

    @McpTool(name = "kg_relation_statistics", description = "统计指定实体的关系")
    public Map<String, Object> statistics(String entityId) {
        var data = service.statistics(entityId);
        return McpResults.success(data,
                McpResults.summary(data.size(), data.size(), false, "关系统计", List.of()), null);
    }
}
