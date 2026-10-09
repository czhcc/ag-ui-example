package com.ac.mcp.server.tool.phase2;

import com.ac.mcp.server.service.PhaseTwoFixtureService;
import com.ac.mcp.server.tool.McpResults;
import com.ac.mcp.server.tool.PhaseTwoResults;
import java.util.Map;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeOverviewTool {
    private final PhaseTwoFixtureService service;

    public KnowledgeOverviewTool(PhaseTwoFixtureService service) { this.service = service; }

    @McpTool(name = "kg_knowledge_overview", description = "查询演示知识库的数据概览：人员实体数、组织节点数和证据记录数，推荐使用指标卡展示")
    public Map<String, Object> query() {
        var data = service.knowledgeOverview();
        var view = McpResults.view("knowledge-overview", "metric", "single", "演示知识库概览", null,
                Map.of("label", "label", "value", "value", "unit", "unit"), Map.of("limit", 12), 10);
        return PhaseTwoResults.result(data, "演示知识库数据概览", data.size(), view, Map.of());
    }
}
