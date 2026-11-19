package com.ac.mcpapps.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Map;

/** Narrow wire adapter for MCP SDK 0.17, whose ClientCapabilities lacks the extensions field. */
public final class McpAppsInitialization {
    private static final ObjectMapper JSON = new ObjectMapper();
    private McpAppsInitialization() {}
    public static void customize(HttpRequest.Builder request, String method, String body) {
        if (body == null || !body.contains("initialize")) return;
        try {
            var root = JSON.readTree(body);
            if (!"initialize".equals(root.path("method").asText())) return;
            ObjectNode capabilities = (ObjectNode) root.path("params").path("capabilities");
            ObjectNode extensions = capabilities.has("extensions") ? (ObjectNode) capabilities.get("extensions") : capabilities.putObject("extensions");
            extensions.set("io.modelcontextprotocol/ui", JSON.valueToTree(Map.of("mimeTypes", List.of("text/html;profile=mcp-app"))));
            request.method(method, HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(root)));
        } catch (Exception e) { throw new IllegalStateException("MCP Apps capability negotiation failed", e); }
    }
}
