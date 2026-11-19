package com.ac.mcp.server;

import com.ac.mcp.server.app.FundFlowAppConfiguration;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FundFlowAppTest {
    private McpSchema.CallToolRequest request(String name, Map<String, Object> args) {
        return McpSchema.CallToolRequest.builder().name(name).arguments(args)
                .meta(Map.of("ac.subject", Map.of("tenantId", "tenant", "userId", "user"))).build();
    }
    @SuppressWarnings("unchecked") private Map<String, Object> data(McpSchema.CallToolResult result) {
        return (Map<String, Object>) ((Map<?, ?>) result.structuredContent()).get("data");
    }
    @Test void filtersPersistAcrossQueriesAndCursorsBindToSnapshotAndAccount() {
        var config = new FundFlowAppConfiguration();
        String id = (String) data(config.execute("kg_fund_flow_graph", request("kg_fund_flow_graph", Map.of(
                "entityId", "entity", "startTime", "2026-09-01T00:00:00+08:00", "endTime", "2026-10-01T00:00:00+08:00")))).get("analysisId");
        var first = data(config.execute("kg_fund_flow_transactions", request("kg_fund_flow_transactions", Map.of("analysisId", id))));
        String cursor = (String) ((Map<?, ?>) first.get("page")).get("nextCursor");
        assertFalse(cursor.isEmpty());
        assertTrue(config.execute("kg_fund_flow_transactions", request("kg_fund_flow_transactions",
                Map.of("analysisId", id, "accountId", "account_001", "cursor", cursor))).isError());
        config.execute("kg_fund_flow_filter", request("kg_fund_flow_filter", Map.of("analysisId", id,
                "startTime", "2026-09-01T00:00:00+08:00", "endTime", "2026-10-01T00:00:00+08:00", "minimumAmount", "3000")));
        assertTrue(config.execute("kg_fund_flow_transactions", request("kg_fund_flow_transactions", Map.of("analysisId", id, "cursor", cursor))).isError());
        var filtered = data(config.execute("kg_fund_flow_transactions", request("kg_fund_flow_transactions", Map.of("analysisId", id))));
        assertEquals(2, ((List<?>) filtered.get("transactions")).size());
        var expanded = data(config.execute("kg_fund_flow_expand", request("kg_fund_flow_expand", Map.of("analysisId", id, "accountId", "account_001"))));
        assertEquals(2, ((List<?>) expanded.get("transactions")).size());
        assertEquals("3000", ((Map<?, ?>) expanded.get("filters")).get("minimumAmount"));
    }
    @Test void resourcesAndToolVisibilityArePublishedByServer() {
        var config = new FundFlowAppConfiguration();
        var tools = config.fundFlowTools();
        assertEquals(4, tools.size());
        assertEquals(FundFlowAppConfiguration.URI, ((Map<?, ?>) tools.get(0).tool().meta().get("ui")).get("resourceUri"));
        assertEquals(List.of("app"), ((Map<?, ?>) tools.get(1).tool().meta().get("ui")).get("visibility"));
        var resource = config.fundFlowResources().get(0).readHandler().apply(null, new McpSchema.ReadResourceRequest(FundFlowAppConfiguration.URI));
        assertTrue(((McpSchema.TextResourceContents) resource.contents().get(0)).text().contains("<!DOCTYPE html>"));
    }
    @Test void analysisIsSubjectScopedAndTimeRangeMustBeValid() {
        var config = new FundFlowAppConfiguration();
        var args = Map.<String, Object>of("entityId", "entity", "startTime", "2026-09-01T00:00:00+08:00", "endTime", "2026-10-01T00:00:00+08:00");
        var request = McpSchema.CallToolRequest.builder().name("kg_fund_flow_graph").arguments(args).meta(Map.of("ac.subject", Map.of("tenantId", "tenant", "userId", "user"))).build();
        var result = config.execute("kg_fund_flow_graph", request);
        assertFalse(Boolean.TRUE.equals(result.isError()));
        var data = (Map<?, ?>) ((Map<?, ?>) result.structuredContent()).get("data");
        var other = McpSchema.CallToolRequest.builder().name("kg_fund_flow_expand").arguments(Map.of("analysisId", data.get("analysisId"), "accountId", "account_001"))
                .meta(Map.of("ac.subject", Map.of("tenantId", "tenant", "userId", "other"))).build();
        assertTrue(config.execute("kg_fund_flow_expand", other).isError());
    }
}
