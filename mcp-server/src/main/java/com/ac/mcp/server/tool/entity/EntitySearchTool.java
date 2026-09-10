package com.ac.mcp.server.tool.entity;

import com.ac.mcp.server.service.EntityService;
import com.ac.mcp.server.tool.McpResults;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class EntitySearchTool {
    private final EntityService service;
    public EntitySearchTool(EntityService service) { this.service = service; }

    @McpTool(name = "kg_search_entity", description = "按关键词查询实体")
    public Map<String, Object> search(String keyword) {
        var data = service.search(keyword);
        return McpResults.success(data,
                McpResults.summary(data.size(), data.size(), false, "实体搜索结果", List.of()), null);
    }
}
