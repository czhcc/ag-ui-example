package com.ac.mcp.server.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** Stable fictional data for phase-two contract and UI verification. */
@Service
@Profile({"demo", "test"})
public class PhaseTwoFixtureService {
    public List<Map<String, Object>> metrics(String personId) {
        if (!"person-001".equals(personId)) return List.of();
        return List.of(row("label", "活动总数", "value", 16, "unit", "次"),
                row("label", "涉及城市", "value", 3, "unit", "个"));
    }

    public List<Map<String, Object>> cards(String keyword) {
        if (!"person".equalsIgnoreCase(keyword)) return List.of();
        return List.of(row("id", "person-001", "name", "张三", "kind", "PERSON", "summary", "示例人员"),
                row("id", "person-002", "name", "李四", "kind", "PERSON", "summary", "示例关联人员"));
    }

    public List<Map<String, Object>> tree(String rootId, String snapshot) {
        if (!"org-root".equals(rootId)) return List.of();
        var rows = new ArrayList<Map<String, Object>>();
        rows.add(row("nodeId", "org-root", "parentNodeId", null, "nodeName", "示例机构", "hasChildren", true));
        rows.add(row("nodeId", "org-dev", "parentNodeId", "org-root", "nodeName", "研发部", "hasChildren", true));
        if ("expanded".equals(snapshot))
            rows.add(row("nodeId", "org-data", "parentNodeId", "org-dev", "nodeName", "数据组", "hasChildren", false));
        return List.copyOf(rows);
    }

    public List<Map<String, Object>> heatmap(String personId) {
        if (!"person-001".equals(personId)) return List.of();
        return List.of(
                cell("北京", "BUSINESS", 4), cell("北京", "WORK", 2),
                cell("北京", "SOCIAL", 1), cell("北京", "TRAVEL", 1),
                cell("上海", "BUSINESS", 3), cell("上海", "WORK", 1),
                cell("上海", "SOCIAL", 1), cell("杭州", "BUSINESS", 2),
                cell("杭州", "WORK", 1));
    }

    public List<Map<String, Object>> path(String sourceId, String targetId) {
        if (!"person-001".equals(sourceId) || !"org-root".equals(targetId)) return List.of();
        return List.of(row("step", 1, "sourceId", "person-001", "targetId", "person-002", "relationType", "KNOWS"),
                row("step", 2, "sourceId", "person-002", "targetId", "org-root", "relationType", "WORKS_AT"));
    }

    public List<Map<String, Object>> evidence(String entityId) {
        if (!"person-001".equals(entityId)) return List.of();
        return List.of(row("step", 1, "evidenceId", "ev-001", "title", "登记记录", "source", "mock-registry",
                        "summary", "测试用登记摘要", "evidenceRef", "ev-001"),
                row("step", 2, "evidenceId", "ev-002", "title", "活动记录", "source", "mock-activity",
                        "summary", "测试用活动摘要", "evidenceRef", "ev-002"));
    }

    private Map<String, Object> cell(String city, String type, int count) {
        return row("city", city, "eventType", type, "count", count);
    }

    private Map<String, Object> row(Object... values) {
        var row = new LinkedHashMap<String, Object>();
        for (int i = 0; i < values.length; i += 2) row.put((String) values[i], values[i + 1]);
        return row;
    }
}
