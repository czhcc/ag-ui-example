package com.ac.mcp.server.tool.entity;

import com.ac.mcp.contract.result.*;
import com.ac.mcp.server.domain.Entity;
import com.ac.mcp.server.service.EntityService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

@Component
public class EntityDetailTool {
    private final EntityService service;
    public EntityDetailTool(EntityService service) { this.service = service; }
    @McpTool(name = "kg_get_entity", description = "获取实体详情", generateOutputSchema = true)
    public McpResult<Entity> get(String entityId) {
        return McpResult.success(service.get(entityId), new ResultSummary(1, 1, false, "实体详情", null), null, null);
    }
}
