package com.ac.mcp.server.tool;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds McpResult JSON payloads per mcp-contract/schema/mcp-result.schema.json.
 * Tools return raw Maps instead of binding to contract classes, demonstrating
 * that the wire contract is the JSON structure itself.
 */
public final class McpResults {
    private McpResults() { }

    public static Map<String, Object> success(Object data, Map<String, Object> summary,
                                              Map<String, Object> presentation) {
        return envelope(true, data, summary, presentation, null);
    }

    public static Map<String, Object> failure(String code, String message, boolean retryable) {
        return envelope(false, null, null, presentation("NONE", List.of()),
                Map.of("code", code, "message", message, "retryable", retryable));
    }

    public static Map<String, Object> envelope(boolean success, Object data, Map<String, Object> summary,
                                               Map<String, Object> presentation, Map<String, Object> error) {
        var result = new LinkedHashMap<String, Object>();
        result.put("specVersion", "1.0");
        result.put("success", success);
        result.put("data", data);
        result.put("summary", summary);
        result.put("presentation", presentation == null ? presentation("NONE", List.of()) : presentation);
        result.put("resultMeta", meta());
        result.put("error", error);
        return result;
    }

    public static Map<String, Object> summary(int count, int total, boolean truncated,
                                              String description, List<String> highlights) {
        var summary = new LinkedHashMap<String, Object>();
        summary.put("count", count);
        summary.put("total", total);
        summary.put("truncated", truncated);
        summary.put("description", description);
        summary.put("highlights", highlights == null ? List.of() : highlights);
        return summary;
    }

    public static Map<String, Object> presentation(String mode, List<Map<String, Object>> views) {
        var presentation = new LinkedHashMap<String, Object>();
        presentation.put("mode", mode);
        presentation.put("views", views == null ? List.of() : views);
        return presentation;
    }

    public static Map<String, Object> view(String id, String type, String subType, String title, String description,
                                           Map<String, String> mapping, Map<String, Object> options, int priority) {
        var view = new LinkedHashMap<String, Object>();
        view.put("id", id);
        view.put("type", type);
        if (subType != null) view.put("subType", subType);
        if (title != null) view.put("title", title);
        if (description != null) view.put("description", description);
        view.put("mapping", mapping);
        view.put("options", options == null ? Map.of() : options);
        view.put("priority", priority);
        return view;
    }

    public static Map<String, Object> recommended(Map<String, Object> view) {
        return presentation("RECOMMENDED", List.of(view));
    }

    private static Map<String, Object> meta() {
        return Map.of("requestId", UUID.randomUUID().toString(),
                "generatedAt", Instant.now().toString());
    }
}
