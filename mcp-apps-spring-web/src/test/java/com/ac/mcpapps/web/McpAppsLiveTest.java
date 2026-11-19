package com.ac.mcpapps.web;

import com.ac.mcpapps.core.*;
import com.ac.richui.core.context.*;
import com.ac.richui.core.event.RuntimeEventSink;
import com.ac.richui.core.observation.DefaultObservationBuilder;
import com.ac.richui.core.result.*;
import com.ac.richui.core.tool.ToolIdentity;
import com.ac.richui.mcp.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.web.reactive.server.WebTestClient;
import static org.junit.jupiter.api.Assertions.*;

/** SDK 0.17 Host interoperates with the separately running SDK 2.0 MCP Server. No model is called. */
@EnabledIfEnvironmentVariable(named = "MCP_APPS_TEST_URL", matches = ".+")
class McpAppsLiveTest {
    @Test void realServerToolsResourcesHostAuthorizationAndFollowup() {
        ObjectMapper json = new ObjectMapper();
        var transport = HttpClientStreamableHttpTransport.builder(System.getenv("MCP_APPS_TEST_URL")).endpoint("/mcp")
                .httpRequestCustomizer((request, method, endpoint, body, context) -> McpAppsInitialization.customize(request, method, body)).build();
        try (var client = McpClient.sync(transport).build()) {
            client.initialize();
            McpAppCatalog catalog = new McpAppCatalog();
            for (var tool : client.listTools().tools()) catalog.register("knowledge", tool.name(), tool.title(),
                    json.convertValue(tool.inputSchema(), Map.class), tool.meta(), tool.annotations() != null && Boolean.TRUE.equals(tool.annotations().readOnlyHint()));
            assertFalse(catalog.required("knowledge", "kg_fund_flow_expand").modelVisible());
            catalog.transport("knowledge", new McpAppTransport() {
                public Map<String, Object> readResource(String uri) {return json.convertValue(client.readResource(new McpSchema.ReadResourceRequest(uri)), Map.class);}
                public Map<String, Object> callTool(String name, Map<String, Object> args, AccessSubject subject) {
                    return json.convertValue(client.callTool(McpSchema.CallToolRequest.builder().name(name).arguments(args)
                            .meta(Map.of("ac.subject", Map.of("tenantId", subject.tenantId(), "userId", subject.userId()))).build()), Map.class);
                }
            });
            var store = new InMemoryResultStore(Duration.ofMinutes(45), 100);
            var processor = new DefaultRichToolResultProcessor(new DefaultMcpResultDecoder(json), store, new DefaultObservationBuilder());
            var apps = new McpAppService(catalog, new McpAppResourceService(catalog, json), new InMemoryMcpAppInstanceStore(Clock.systemUTC()),
                    (subject, server, tool) -> tool.startsWith("kg_fund_flow_"), store, processor, json, Clock.systemUTC());
            var nativeResult = client.callTool(McpSchema.CallToolRequest.builder().name("kg_fund_flow_graph").arguments(Map.of(
                    "entityId", "entity", "startTime", "2026-09-01T00:00:00+08:00", "endTime", "2026-10-01T00:00:00+08:00"))
                    .meta(Map.of("ac.subject", Map.of("tenantId", "tenant", "userId", "user"))).build());
            var scope = new RunScope("tenant", "user", "thread", "run", "call");
            var raw = new McpCallToolResultAdapter(json).adapt(nativeResult);
            var identity = new ToolIdentity("knowledge", "kg_fund_flow_graph");
            var processed = processor.process(scope, identity, raw);
            var descriptor = apps.create(scope, identity, processed, raw).orElseThrow();
            String id = (String) descriptor.get("appInstanceId");
            var subject = AccessSubject.of("tenant", "user");
            assertTrue(apps.resource(id, subject).html().contains("资金流"));
            var web = WebTestClient.bindToController(new McpAppsController(apps,
                    request -> AccessSubject.of("tenant", request.getHeaders().getFirst("X-User-Id")))).build();
            web.get().uri("/api/mcp-apps/instances/" + id + "/resource").header("X-User-Id", "other").exchange().expectStatus().isForbidden();
            web.get().uri("/api/mcp-apps/instances/" + id + "/resource").header("X-User-Id", "user").exchange().expectStatus().isOk();
            var analysis = (String) ((Map<?, ?>) raw.structuredContent().get("data")).get("analysisId");
            var query = apps.invoke(id, subject, "request", 1, "kg_fund_flow_expand", Map.of("analysisId", analysis, "accountId", "account_001"));
            assertEquals(2L, query.get("revision"));
            apps.context(id, subject, Map.of("selectedEntityIds", List.of("account_003")));
            String ref = (String) apps.followup(id, subject, "分析账户 C").get("appFollowupRef");
            assertTrue(apps.consumeFollowup(ref, new RunScope("tenant", "user", "thread", "new", null)).contains("account_003"));
            apps.close(id, subject);
            assertThrows(McpAppException.class, () -> apps.latestResult(id, subject));
        }
    }
}
