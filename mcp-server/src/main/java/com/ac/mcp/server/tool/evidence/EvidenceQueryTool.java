package com.ac.mcp.server.tool.evidence;

import com.ac.mcp.server.service.EvidenceService;
import com.ac.mcp.server.tool.McpResults;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class EvidenceQueryTool {
    private final EvidenceService service;
    public EvidenceQueryTool(EvidenceService service) { this.service = service; }

    @McpTool(name = "kg_get_evidence", description = "获取实体相关证据")
    public Map<String, Object> query(String entityId) {
        var data = service.query(entityId);
        return McpResults.success(data,
                McpResults.summary(data.size(), data.size(), false, "证据查询结果", List.of()), null);
    }
}
