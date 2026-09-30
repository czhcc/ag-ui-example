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
public class RelationshipPathTool {
    private final PhaseTwoFixtureService service;
    public RelationshipPathTool(PhaseTwoFixtureService service) { this.service = service; }

    @McpTool(name = "kg_relationship_path", description = "查询关联路径并返回推荐可视化视图")
    public Map<String, Object> query(@ToolParam(description = "起点ID") String sourceId, @ToolParam(description = "终点ID") String targetId) {
        if (PhaseTwoResults.invalid(sourceId) || PhaseTwoResults.invalid(targetId)) return McpResults.failureV12("INVALID_ARGUMENT", "Required parameter is missing or too long");

        var data = service.path(sourceId, targetId);
        var view = McpResults.view("person-org-path", "relationship_path", "ordered", "关联路径", null,
                Map.of("step", "step", "source", "sourceId", "target", "targetId", "label", "relationType"), Map.of("limit", 30, "showLabels", true), 10);
        return PhaseTwoResults.result(data, "关联路径", data.size(), view, Map.of("sourceId", sourceId, "targetId", targetId));
    }
}
