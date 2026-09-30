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
public class EntityCardsTool {
    private final PhaseTwoFixtureService service;
    public EntityCardsTool(PhaseTwoFixtureService service) { this.service = service; }

    @McpTool(name = "kg_entity_cards", description = "查询实体检索结果并返回推荐可视化视图")
    public Map<String, Object> query(@ToolParam(description = "搜索关键词") String keyword) {
        if (PhaseTwoResults.invalid(keyword)) return McpResults.failureV12("INVALID_ARGUMENT", "Required parameter is missing or too long");

        var data = service.cards(keyword);
        var view = McpResults.view("entity-cards", "entity_card", "standard", "实体检索结果", null,
                Map.of("id", "id", "name", "name", "kind", "kind", "summary", "summary"), Map.of("limit", 50, "showSummary", true), 10);
        return PhaseTwoResults.result(data, "实体检索结果", data.size(), view, Map.of("keyword", keyword));
    }
}
