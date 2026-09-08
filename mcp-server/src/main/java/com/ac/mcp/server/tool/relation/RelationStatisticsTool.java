package com.ac.mcp.server.tool.relation;

import com.ac.mcp.contract.result.*;
import com.ac.mcp.server.domain.Relation;
import com.ac.mcp.server.service.RelationService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class RelationStatisticsTool {
    private final RelationService service;
    public RelationStatisticsTool(RelationService service) { this.service = service; }
    @McpTool(name = "kg_relation_statistics", description = "统计指定实体的关系", generateOutputSchema = true)
    public McpResult<List<Relation>> statistics(String entityId) {
        var data = service.statistics(entityId);
        return McpResult.success(data, new ResultSummary(data.size(), data.size(), false, "关系统计", List.of()), null, null);
    }
}
