package com.ac.mcp.server.tool.entity;

import com.ac.mcp.contract.result.*;
import com.ac.mcp.server.domain.Entity;
import com.ac.mcp.server.service.EntityService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class EntitySearchTool {
    private final EntityService service;
    public EntitySearchTool(EntityService service) { this.service = service; }
    @McpTool(name = "kg_search_entity", description = "按关键词查询实体", generateOutputSchema = true)
    public McpResult<List<Entity>> search(String keyword) {
        var data = service.search(keyword);
        return McpResult.success(data, new ResultSummary(data.size(), data.size(), false, "实体搜索结果", List.of()), null, null);
    }
}
