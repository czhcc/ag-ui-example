package com.ac.mcp.server.app;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;

/** Deterministic demonstration App. Production business identity must come from authenticated MCP transport. */
@Configuration
public class FundFlowAppConfiguration {
    public static final String URI = "ui://kg/fund-flow/v1/index.html";
    private record Analysis(String owner, String entity, String start, String end, String minimumAmount,
                            int depth, long revision, Instant expires) {}
    private final Map<String, Analysis> analyses = new ConcurrentHashMap<>();

    @Bean public List<McpServerFeatures.SyncToolSpecification> fundFlowTools() {
        List<McpServerFeatures.SyncToolSpecification> tools = new ArrayList<>();
        tools.add(tool("kg_fund_flow_graph", "资金流分析（演示数据）", true,
                schema(Map.of("entityId", string(), "startTime", time(), "endTime", time()), "entityId", "startTime", "endTime")));
        tools.add(tool("kg_fund_flow_expand", "展开资金账户", false,
                schema(Map.of("analysisId", string(), "accountId", string()), "analysisId", "accountId")));
        tools.add(tool("kg_fund_flow_filter", "筛选资金交易", false,
                schema(Map.of("analysisId", string(), "startTime", time(), "endTime", time(),
                        "minimumAmount", Map.of("type", "string", "maxLength", 24)), "analysisId", "startTime", "endTime")));
        tools.add(tool("kg_fund_flow_transactions", "分页资金交易明细", false,
                schema(Map.of("analysisId", string(), "accountId", string(), "cursor", string()), "analysisId")));
        return tools;
    }
    private McpServerFeatures.SyncToolSpecification tool(String name, String title, boolean entrance, Map<String, Object> schema) {
        var ui = entrance ? Map.of("resourceUri", URI, "visibility", List.of("model", "app"))
                : Map.of("visibility", List.of("app"));
        var tool = McpSchema.Tool.builder().name(name).title(title).description(title)
                .inputSchema(schema).meta(Map.of("ui", ui))
                .annotations(new McpSchema.ToolAnnotations(title, true, false, true, false, false)).build();
        return new McpServerFeatures.SyncToolSpecification(tool, (exchange, request) -> execute(name, request));
    }
    @Bean public List<McpServerFeatures.SyncResourceSpecification> fundFlowResources() {
        var resource = McpSchema.Resource.builder().uri(URI).name("fund-flow-app").mimeType("text/html;profile=mcp-app").build();
        return List.of(new McpServerFeatures.SyncResourceSpecification(resource, (exchange, request) -> {
            try {
                String html = new ClassPathResource("mcp-apps/fund-flow/v1/index.html").getContentAsString(StandardCharsets.UTF_8);
                return new McpSchema.ReadResourceResult(List.of(new McpSchema.TextResourceContents(URI,
                        "text/html;profile=mcp-app", html, Map.of("ui", Map.of("csp", Map.of(
                                "connectDomains", List.of(), "resourceDomains", List.of()), "prefersBorder", true)))));
            } catch (IOException e) { throw new IllegalStateException("Build the fund-flow App before packaging mcp-server", e); }
        }));
    }
    public synchronized McpSchema.CallToolResult execute(String name, McpSchema.CallToolRequest request) {
        try {
            var args = request.arguments() == null ? Map.<String, Object>of() : request.arguments();
            var subject = request.meta() == null ? null : request.meta().get("ac.subject");
            if (!(subject instanceof Map<?, ?> owner) || !(owner.get("tenantId") instanceof String tenant)
                    || !(owner.get("userId") instanceof String user) || tenant.isBlank() || user.isBlank()) return failure("MISSING_SUBJECT");
            String ownerId = tenant + "\n" + user;
            analyses.entrySet().removeIf(e -> !e.getValue().expires.isAfter(Instant.now()));
            String id;
            Analysis analysis;
            if (name.equals("kg_fund_flow_graph")) {
                if (analyses.size() >= 1000) return failure("QUOTA_EXCEEDED");
                id = "analysis_" + UUID.randomUUID();
                analysis = new Analysis(ownerId, required(args, "entityId"), required(args, "startTime"), required(args, "endTime"), "0", 1, 1, Instant.now().plusSeconds(1800));
                validateTime(analysis.start, analysis.end); analyses.put(id, analysis);
            } else {
                id = required(args, "analysisId"); analysis = analyses.get(id);
                if (analysis == null || !analysis.owner.equals(ownerId)) return failure("ANALYSIS_DENIED");
            }
            if (args.containsKey("accountId") && !Set.of("account_001", "account_002", "account_003").contains(args.get("accountId"))) return failure("ACCOUNT_DENIED");
            if (name.equals("kg_fund_flow_filter")) {
                validateTime(required(args, "startTime"), required(args, "endTime"));
                if (args.containsKey("minimumAmount")) {
                    BigDecimal amount = new BigDecimal(required(args, "minimumAmount"));
                    if (amount.signum() < 0) return failure("INVALID_ARGUMENT");
                }
                analysis = new Analysis(ownerId, analysis.entity, required(args, "startTime"), required(args, "endTime"),
                        args.containsKey("minimumAmount") ? required(args, "minimumAmount") : "0",
                        analysis.depth, analysis.revision + 1, analysis.expires);
                analyses.put(id, analysis);
            }
            if (name.equals("kg_fund_flow_expand")) {
                analysis = new Analysis(ownerId, analysis.entity, analysis.start, analysis.end, analysis.minimumAmount, 2, analysis.revision + 1, analysis.expires);
                analyses.put(id, analysis);
            }
            List<Map<String, Object>> transactions = new ArrayList<>();
            String[] accounts = {"account_001", "account_002", "account_003"};
            String[] amounts = {"12000.00", "3800.50", "950.00", "2400.00", "120.00", "60.00"};
            String[] currencies = {"CNY", "CNY", "USD", "CNY", "USD", "CNY"};
            for (int i = 0; i < 6; i++) {
                var time = OffsetDateTime.parse("2026-09-" + String.format("%02d", i + 2) + "T12:00:00+08:00");
                if (time.isBefore(OffsetDateTime.parse(analysis.start)) || !time.isBefore(OffsetDateTime.parse(analysis.end))) continue;
                if (new BigDecimal(amounts[i]).compareTo(new BigDecimal(analysis.minimumAmount)) < 0) continue;
                String source = accounts[i % 3], target = accounts[(i + 1) % 3];
                if (name.equals("kg_fund_flow_transactions") && args.containsKey("accountId")
                        && !source.equals(args.get("accountId")) && !target.equals(args.get("accountId"))) continue;
                transactions.add(Map.of("id", "tx_" + i, "source", source, "target", target,
                        "amount", amounts[i], "currency", currencies[i], "time", time.toString()));
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("analysisId", id); data.put("entityId", analysis.entity); data.put("demo", true);
            data.put("filters", Map.of("startTime", analysis.start, "endTime", analysis.end, "minimumAmount", analysis.minimumAmount));
            data.put("dataRevision", analysis.revision);
            data.put("generatedAt", Instant.now().toString());
            if (name.equals("kg_fund_flow_transactions")) {
                int offset = 0;
                if (args.containsKey("cursor") && !((String) args.get("cursor")).isBlank()) {
                    String[] cursor = ((String) args.get("cursor")).split(":");
                    if (cursor.length != 4 || !cursor[0].equals(id) || !cursor[1].equals(Long.toString(analysis.revision))
                            || !cursor[2].equals(args.getOrDefault("accountId", "*"))) return failure("INVALID_CURSOR");
                    offset = Integer.parseInt(cursor[3]);
                }
                if (offset < 0 || offset > transactions.size()) return failure("INVALID_CURSOR");
                int end = Math.min(offset + 2, transactions.size());
                data.put("transactions", transactions.subList(offset, end));
                data.put("page", Map.of("hasMore", end < transactions.size(), "nextCursor", end < transactions.size()
                        ? id + ":" + analysis.revision + ":" + args.getOrDefault("accountId", "*") + ":" + end : ""));
            } else {
                List<Map<String, Object>> nodes = new ArrayList<>();
                for (int i = 0; i < (analysis.depth == 1 ? 2 : 3); i++) nodes.add(Map.of("id", accounts[i], "label", "账户 " + (char) ('A' + i)));
                data.put("nodes", nodes);
                data.put("edges", transactions.stream().filter(tx -> analysisDepthContains(nodes, tx)).toList());
                data.put("transactions", transactions);
                data.put("page", Map.of("hasMore", false, "nextCursor", ""));
            }
            return McpSchema.CallToolResult.builder().addTextContent("资金流演示数据；可在专属界面查询和继续分析。")
                    .structuredContent(Map.of("specVersion", "1.2", "success", true, "data", data,
                            "summary", Map.of("description", "资金流演示：" + transactions.size() + " 笔交易；不同币种分别统计", "truncated", false),
                            "presentation", Map.of("mode", "NONE", "views", List.of()))).isError(false).build();
        } catch (Exception e) { return failure("INVALID_ARGUMENT"); }
    }
    private boolean analysisDepthContains(List<Map<String, Object>> nodes, Map<String, Object> tx) {
        var ids = nodes.stream().map(n -> n.get("id")).toList();
        return ids.contains(tx.get("source")) && ids.contains(tx.get("target"));
    }
    private McpSchema.CallToolResult failure(String code) { return McpSchema.CallToolResult.builder()
            .addTextContent("资金流查询失败：" + code).isError(true).build(); }
    private String required(Map<String, Object> args, String key) {
        if (!(args.get(key) instanceof String s) || s.isBlank() || s.length() > 128) throw new IllegalArgumentException();
        return s;
    }
    private void validateTime(String start, String end) { if (!OffsetDateTime.parse(start).isBefore(OffsetDateTime.parse(end))) throw new IllegalArgumentException(); }
    private Map<String, Object> string() { return Map.of("type", "string", "minLength", 1, "maxLength", 128); }
    private Map<String, Object> time() { return Map.of("type", "string", "format", "date-time", "maxLength", 64); }
    private Map<String, Object> schema(Map<String, Object> properties, String... required) {
        return Map.of("type", "object", "properties", properties, "required", List.of(required), "additionalProperties", false);
    }
}
