package com.ac.mcp.server.tool.phase2;

import com.ac.mcp.server.service.PhaseTwoFixtureService;
import com.ac.mcp.server.tool.McpResults;
import com.ac.mcp.server.tool.PhaseTwoResults;
import java.util.Map;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"demo", "test"})
public class EvidenceChainTool {
    private final PhaseTwoFixtureService service;
    public EvidenceChainTool(PhaseTwoFixtureService service) { this.service = service; }

    @McpTool(name = "kg_evidence_chain", description = "查询证据链并返回推荐可视化视图")
    public Map<String, Object> query(@ToolParam(description = "实体ID") String entityId) {
        if (PhaseTwoResults.invalid(entityId)) return McpResults.failureV12("INVALID_ARGUMENT", "Required parameter is missing or too long");

        var data = service.evidence(entityId);
        var view = McpResults.view("person-evidence", "evidence_chain", "ordered", "证据链", null,
                Map.of("step", "step", "id", "evidenceId", "title", "title", "source", "source", "summary", "summary", "evidenceRef", "evidenceRef"), Map.of("limit", 50, "showSource", true), 10);
        return PhaseTwoResults.result(data, "证据链", data.size(), view, Map.of("entityId", entityId));
    }
}
