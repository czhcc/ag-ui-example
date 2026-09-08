package com.ac.mcp.server.tool.evidence;

import com.ac.mcp.contract.result.*;
import com.ac.mcp.server.domain.Evidence;
import com.ac.mcp.server.service.EvidenceService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class EvidenceQueryTool {
    private final EvidenceService service;
    public EvidenceQueryTool(EvidenceService service) { this.service = service; }
    @McpTool(name = "kg_get_evidence", description = "获取实体相关证据", generateOutputSchema = true)
    public McpResult<List<Evidence>> query(String entityId) {
        var data = service.query(entityId);
        return McpResult.success(data, new ResultSummary(data.size(), data.size(), false, "证据查询结果", List.of()), null, null);
    }
}
