package com.ac.mcp.server.tool.relation;

import com.ac.mcp.server.service.RelationService;
import com.ac.mcp.server.tool.McpResults;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class RelationExpandTool {
    private final RelationService service;
    public RelationExpandTool(RelationService service) { this.service = service; }

    @McpTool(name = "kg_expand_relations", description = "扩展指定实体的关系网络")
    public Map<String, Object> expand(String entityId, int depth) {
        var data = service.expand(entityId, Math.min(Math.max(depth, 1), 3));
        return McpResults.success(data,
                McpResults.summary(data.size(), data.size(), false, "关系扩展结果", List.of()),
                McpResults.recommended(McpResults.view("relation-network", "relation_graph", "network", "实体关系网络", null,
                        Map.of("source", "sourceId", "target", "targetId", "label", "relationType"), Map.of(), 10)));
    }
}
