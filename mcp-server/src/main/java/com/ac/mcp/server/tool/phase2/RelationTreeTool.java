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
public class RelationTreeTool {
    private final PhaseTwoFixtureService service;
    public RelationTreeTool(PhaseTwoFixtureService service) { this.service = service; }

    @McpTool(name = "kg_relation_tree", description = "查询组织结构并返回树形视图。snapshot=base 返回基础层级；snapshot=expanded 返回包含数据组的完整层级。先展示再补充时依次查询 base 和 expanded")
    public Map<String, Object> query(@ToolParam(description = "根节点 ID") String rootId, @ToolParam(required = false, description = "base=基础层级（默认），expanded=完整层级") String snapshot) {
        if (PhaseTwoResults.invalid(rootId)) return McpResults.failureV12("INVALID_ARGUMENT", "Required parameter is missing or too long");
        if (snapshot == null || snapshot.isBlank()) snapshot = "base";
        if (!"base".equals(snapshot) && !"expanded".equals(snapshot)) return McpResults.failureV12("INVALID_ARGUMENT", "snapshot must be base or expanded");
        var data = service.tree(rootId, snapshot);
        var view = McpResults.view("org-tree", "tree", "hierarchy", "组织结构", null,
                Map.of("id", "nodeId", "parentId", "parentNodeId", "label", "nodeName", "hasChildren", "hasChildren"), Map.of("limit", 500), 10,
                McpResults.drillDown("nodeId", "查看子节点详情", "展示 {rootId} 下 {nodeId} 的完整子节点"));
        return PhaseTwoResults.result(data, "组织结构", data.isEmpty() ? 0 : 3, view, Map.of("rootId", rootId, "snapshot", snapshot));
    }
}
