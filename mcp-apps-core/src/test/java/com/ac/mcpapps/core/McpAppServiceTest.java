package com.ac.mcpapps.core;

import com.ac.mcp.contract.result.*;
import com.ac.richui.core.context.*;
import com.ac.richui.core.observation.AgentObservation;
import com.ac.richui.core.result.*;
import com.ac.richui.core.tool.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class McpAppServiceTest {
    final ObjectMapper json = new ObjectMapper();
    final McpAppCatalog catalog = new McpAppCatalog();
    final AtomicBoolean allowed = new AtomicBoolean(true);
    final AtomicInteger calls = new AtomicInteger();
    final AtomicReference<String> html = new AtomicReference<>("<!DOCTYPE html><html><head></head><body>App</body></html>");
    final AccessSubject owner = AccessSubject.of("tenant", "user");
    final RunScope origin = new RunScope("tenant", "user", "thread", "run", "call");
    final ResultStore store = new InMemoryResultStore(Duration.ofMinutes(45), 100);
    final RichToolResultProcessor processor = (scope, tool, raw) -> {
        var envelope = McpResult.success(raw.structuredContent().get("data"), new ResultSummary(1, 1, false, "Verified summary", List.of()), null, null);
        return new ProcessedToolResult(ProcessedToolResult.Kind.RICH_RESULT, new AgentObservation("Safe summary"), store.save(scope, tool, envelope), "Safe summary");
    };
    McpAppService service;
    String id;
    Map<String, Object> result() { return Map.of("success", true, "data", Map.of("analysisId", "analysis", "entityId", "entity",
            "nodes", List.of(Map.of("id", "account")))); }
    @BeforeEach void setup() {
        catalog.register("server", "entry", "App", Map.of("type", "object"), Map.of("ui", Map.of("resourceUri", "ui://app/v1/index.html")), true);
        catalog.register("server", "query", "Query", Map.of("type", "object", "properties", Map.of(
                "analysisId", Map.of("type", "string"), "accountId", Map.of("type", "string")), "required", List.of("analysisId")),
                Map.of("ui", Map.of("visibility", List.of("app"))), true);
        catalog.transport("server", new McpAppTransport() {
            public Map<String, Object> readResource(String uri) { return Map.of("contents", List.of(Map.of("uri", uri, "mimeType", "text/html;profile=mcp-app", "text", html.get()))); }
            public Map<String, Object> callTool(String name, Map<String, Object> args, AccessSubject subject) {
                assertEquals(owner, subject); calls.incrementAndGet();
                return Map.of("structuredContent", result(), "content", List.of(Map.of("type", "text", "text", "Result")));
            }
        });
        var resources = new McpAppResourceService(catalog, json);
        Clock testClock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
        service = new McpAppService(catalog, resources, new InMemoryMcpAppInstanceStore(testClock),
                (subject, server, tool) -> allowed.get(), store, processor, json, testClock);
        var raw = new RawToolResult(false, result(), "Result");
        var identity = new ToolIdentity("server", "entry");
        var descriptor = service.create(origin, identity, processor.process(origin, identity, raw), raw).orElseThrow();
        id = (String) descriptor.get("appInstanceId");
    }
    @Test void rejectsOtherUsersAndRevokedPoliciesOnEveryRead() {
        assertThrows(McpAppException.class, () -> service.resource(id, AccessSubject.of("tenant", "other")));
        assertThrows(McpAppException.class, () -> service.snapshot(id, AccessSubject.of("other", "user")));
        allowed.set(false);
        assertThrows(McpAppException.class, () -> service.initialResult(id, owner));
    }
    @Test void validatesBusinessIdsSchemaAndToolBeforeInvoking() {
        for (var args : List.of(Map.of("analysisId", "other"), Map.of("analysisId", "analysis", "accountId", "foreign"),
                Map.of("analysisId", "analysis", "unexpected", "injected")))
            assertThrows(McpAppException.class, () -> service.invoke(id, owner, "request", 1, "query", new HashMap<>(args)));
        assertThrows(McpAppException.class, () -> service.invoke(id, owner, "request", 1, "delete", Map.of()));
        assertEquals(0, calls.get());
    }
    @Test void idempotentQueryDoesNotRepeatSideEffectsAndRejectsStaleRevision() {
        var args = Map.<String, Object>of("analysisId", "analysis", "accountId", "account");
        var first = service.invoke(id, owner, "request", 1, "query", args);
        assertEquals(first, service.invoke(id, owner, "request", 1, "query", args));
        assertEquals(1, calls.get());
        assertThrows(McpAppException.class, () -> service.invoke(id, owner, "other", 1, "query", args));
        assertThrows(McpAppException.class, () -> service.invoke(id, owner, "request", 2, "query", Map.of("analysisId", "analysis")));
    }
    @Test void followupsAreScopedSingleUseAndSelectionsMustExist() {
        assertThrows(McpAppException.class, () -> service.context(id, owner, Map.of("selectedEntityIds", List.of("foreign"))));
        service.context(id, owner, Map.of("selectedEntityIds", List.of("account")));
        var ticket = service.followup(id, owner, "Analyze account");
        String ref = (String) ticket.get("appFollowupRef");
        assertThrows(McpAppException.class, () -> service.consumeFollowup(ref, new RunScope("tenant", "user", "other", "new", null)));
        assertTrue(service.consumeFollowup(ref, new RunScope("tenant", "user", "thread", "new", null)).contains("Verified summary"));
        assertThrows(McpAppException.class, () -> service.consumeFollowup(ref, new RunScope("tenant", "user", "thread", "new", null)));
    }
    @Test void resourceVersionCannotChangeAndClosedInstanceCannotQuery() {
        html.set("<!DOCTYPE html><html>changed</html>");
        assertEquals("APP_VERSION_MISMATCH", assertThrows(McpAppException.class, () -> service.resource(id, owner)).code());
        service.close(id, owner);
        assertEquals("CLOSED", service.snapshot(id, owner).get("status"));
        assertThrows(McpAppException.class, () -> service.invoke(id, owner, "request", 1, "query", Map.of("analysisId", "analysis")));
        assertEquals(0, calls.get());
    }
    @Test void rateLimitIsSharedAcrossInstancesAndReturnedResultsCannotMutateStoredState() {
        service.latestResult(id, owner).clear();
        assertTrue(service.latestResult(id, owner).containsKey("structuredContent"));
        var raw = new RawToolResult(false, result(), "Result");
        var identity = new ToolIdentity("server", "entry");
        String other = (String) service.create(origin, identity, processor.process(origin, identity, raw), raw).orElseThrow().get("appInstanceId");
        for (int i = 0; i < 30; i++) service.invoke(i % 2 == 0 ? id : other, owner, "request" + i,
                1 + i / 2, "query", Map.of("analysisId", "analysis"));
        assertEquals("APP_RATE_LIMITED", assertThrows(McpAppException.class,
                () -> service.invoke(other, owner, "overflow", 16, "query", Map.of("analysisId", "analysis"))).code());
        assertEquals(30, calls.get());
    }
}
