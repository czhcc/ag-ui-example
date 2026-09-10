package com.ac.mcp.server.tool.entity;

import com.ac.mcp.server.service.EntityService;
import com.ac.mcp.server.tool.McpResults;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class EntityDetailTool {
    private final EntityService service;
    public EntityDetailTool(EntityService service) { this.service = service; }

    @McpTool(name = "kg_get_entity", description = "获取实体详情")
    public Map<String, Object> get(String entityId) {
        return McpResults.success(service.get(entityId),
                McpResults.summary(1, 1, false, "实体详情", null), null);
    }
}
