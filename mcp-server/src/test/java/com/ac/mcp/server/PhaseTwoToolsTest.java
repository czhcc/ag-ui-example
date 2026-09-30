package com.ac.mcp.server;

import com.ac.mcp.server.service.PhaseTwoFixtureService;
import com.ac.mcp.server.tool.phase2.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The six question fixtures are callable without a model or remote business service. */
class PhaseTwoToolsTest {
    private final PhaseTwoFixtureService data = new PhaseTwoFixtureService();

    @Test void eachQuestionFixtureReturnsTheExpectedView() {
        check(new ActivityMetricsTool(data).query("person-001"), "activity-metrics", "metric", 2);
        check(new EntityCardsTool(data).query("person"), "entity-cards", "entity_card", 2);
        check(new RelationTreeTool(data).query("org-root", "base"), "org-tree", "tree", 2);
        check(new ActivityHeatmapTool(data).query("person-001"), "activity-heatmap", "heatmap", 9);
        check(new RelationshipPathTool(data).query("person-001", "org-root"),
                "person-org-path", "relationship_path", 2);
        check(new EvidenceChainTool(data).query("person-001"), "person-evidence", "evidence_chain", 2);
    }

    @Test void treeBaseAndExpandedAreDistinctSnapshots() {
        var tool = new RelationTreeTool(data);
        var base = tool.query("org-root", "base");
        var expanded = tool.query("org-root", "expanded");
        assertEquals(2, rows(base).size());
        assertEquals(3, rows(expanded).size());
        assertEquals("org-data", rows(expanded).get(2).get("nodeId"));
        assertEquals(true, ((Map<?, ?>) base.get("summary")).get("truncated"));
        assertEquals(false, ((Map<?, ?>) expanded.get("summary")).get("truncated"));
    }

    @Test void unknownAndInvalidInputsStayOutOfPresentation() {
        var empty = new ActivityMetricsTool(data).query("person-999");
        assertTrue(rows(empty).isEmpty());
        assertEquals("NONE", ((Map<?, ?>) empty.get("presentation")).get("mode"));
        var failed = new RelationTreeTool(data).query("org-root", "invalid");
        assertEquals(false, failed.get("success"));
        assertNull(failed.get("data"));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> rows(Map<String, Object> result) {
        return (List<Map<String, Object>>) result.get("data");
    }

    private void check(Map<String, Object> result, String id, String type, int count) {
        assertEquals("1.2", result.get("specVersion"));
        assertEquals(true, result.get("success"));
        assertEquals(count, rows(result).size());
        var presentation = (Map<?, ?>) result.get("presentation");
        assertEquals("RECOMMENDED", presentation.get("mode"));
        var view = (Map<?, ?>) ((List<?>) presentation.get("views")).get(0);
        assertEquals(id, view.get("id"));
        assertEquals(type, view.get("type"));
    }
}
