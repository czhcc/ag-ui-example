package com.ac.richui.core.presentation;

import com.ac.mcp.contract.presentation.*;
import com.ac.mcp.contract.result.McpResult;
import com.ac.richui.core.context.AccessSubject;
import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.result.InMemoryResultStore;
import com.ac.richui.core.tool.ToolIdentity;
import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhaseTwoPresentationTest {
    private final PresentationValidator validator = new PresentationValidator();
    private final TreeViewHint tree = new TreeViewHint("org-tree", "tree", "hierarchy", "Org", null,
            new TreeMapping("nodeId", "parentNodeId", "nodeName", "hasChildren"), Map.of(), 10, null);

    @Test void treeAllowsNullRootButRejectsOrphansAndCycles() {
        assertDoesNotThrow(() -> validator.validate(tree, result(List.of(row("root", null), row("child", "root")), tree)));
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(tree, result(List.of(row("root", null), row("bad", "missing")), tree)));
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(tree, result(List.of(row("root", null), row("a", "b"), row("b", "a")), tree)));
    }

    @Test void heatmapAndPathRejectInvalidShapes() {
        var heatmap = new HeatmapViewHint("heat", "heatmap", "matrix", null, null,
                new HeatmapMapping("x", "y", "value"), Map.of(), 10, null);
        var point = Map.<String, Object>of("x", "Beijing", "y", "WORK", "value", 2);
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(heatmap, result(List.of(point, point), heatmap)));
        var path = new RelationshipPathViewHint("path", "relationship_path", "ordered", null, null,
                new RelationshipPathMapping("step", "source", "target", null), Map.of(), 10, null);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(path, result(List.of(
                Map.of("step", 1, "source", "a", "target", "b"),
                Map.of("step", 2, "source", "c", "target", "d")), path)));
    }

    @Test void rejectsUnknownOrOutOfRangeDisplayOptions() {
        var unknown = new TreeViewHint("org-tree", "tree", "hierarchy", "Org", null,
                tree.mapping(), Map.of("script", "alert(1)"), 10, null);
        var tooLarge = new TreeViewHint("org-tree", "tree", "hierarchy", "Org", null,
                tree.mapping(), Map.of("limit", 501), 10, null);
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(unknown, result(List.of(row("root", null)), unknown)));
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(tooLarge, result(List.of(row("root", null)), tooLarge)));
    }

    @Test void updatePreservesSurfaceAndRejectsOtherRun() {
        var store = new InMemoryResultStore(Duration.ofMinutes(5), 10);
        var service = new DefaultPresentationService(store);
        var scope = new RunScope("tenant", "user", "thread", "run", null);
        var subject = AccessSubject.of("tenant", "user");
        var first = store.save(scope, new ToolIdentity("demo", "tree"), result(List.of(row("root", null)), tree));
        var second = store.save(scope, new ToolIdentity("demo", "tree"), result(List.of(
                row("root", null), row("child", "root")), tree));
        var created = service.render(scope, subject, first, "org-tree");
        assertEquals("1.1", created.profileVersion());
        assertEquals(1, created.revision());
        var updated = service.update(scope, subject, created.surfaceId(), second, "org-tree");
        assertEquals(created.surfaceId(), updated.surfaceId());
        assertEquals(second.value(), updated.dataRef());
        assertEquals(2, updated.revision());
        assertThrows(IllegalArgumentException.class, () -> service.update(
                new RunScope("tenant", "user", "thread", "another", null), subject,
                created.surfaceId(), second, "org-tree"));
    }

    @Test void failedUpdateKeepsRevisionAndRegistryExpires() {
        var now = new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));
        Clock clock = new Clock() {
            @Override public ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(ZoneId zone) { return this; }
            @Override public Instant instant() { return now.get(); }
        };
        var registry = new InMemorySurfaceRegistry(Duration.ofMinutes(5), 10, clock);
        var store = new InMemoryResultStore(Duration.ofMinutes(10), 10);
        var service = new DefaultPresentationService(store, validator, new PresentationMapper(), registry);
        var scope = new RunScope("tenant", "user", "thread", "run", null);
        var subject = AccessSubject.of("tenant", "user");
        var valid = store.save(scope, new ToolIdentity("demo", "tree"), result(List.of(row("root", null)), tree));
        var invalid = store.save(scope, new ToolIdentity("demo", "tree"), result(List.of(row("bad", "missing")), tree));
        var created = service.render(scope, subject, valid, "org-tree");
        assertThrows(IllegalArgumentException.class,
                () -> service.update(scope, subject, created.surfaceId(), invalid, "org-tree"));
        assertEquals(1, registry.replace(created.surfaceId(), entry -> entry).spec().revision());
        now.set(now.get().plus(Duration.ofMinutes(6)));
        assertThrows(IllegalArgumentException.class,
                () -> service.update(scope, subject, created.surfaceId(), valid, "org-tree"));
    }

    private Map<String, Object> row(String id, String parent) {
        var row = new LinkedHashMap<String, Object>();
        row.put("nodeId", id); row.put("parentNodeId", parent);
        row.put("nodeName", id); row.put("hasChildren", false);
        return row;
    }

    private McpResult<List<Map<String, Object>>> result(List<Map<String, Object>> data, ViewHint view) {
        return McpResult.success(data, null, PresentationHint.recommended(view), null);
    }
}
