package com.ac.mcp.server.tool;

import java.util.List;
import java.util.Map;

/** Shared envelope and parameter handling for deterministic demo tools. */
public final class PhaseTwoResults {
    private PhaseTwoResults() { }

    public static Map<String, Object> result(List<Map<String, Object>> data, String description,
                                             int total, Map<String, Object> view,
                                             Map<String, Object> attributes) {
        boolean empty = data.isEmpty();
        return McpResults.successV12(data,
                McpResults.summary(data.size(), empty ? 0 : total, !empty && data.size() < total,
                        description, List.of()),
                empty ? McpResults.presentation("NONE", List.of()) : McpResults.recommended(view),
                attributes);
    }

    public static boolean invalid(String value) {
        return value == null || value.isBlank() || value.length() > 200;
    }
}
