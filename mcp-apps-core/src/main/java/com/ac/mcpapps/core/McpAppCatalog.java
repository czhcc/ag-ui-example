package com.ac.mcpapps.core;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Retains original tool identities and metadata even when model adapters discard metadata. */
public final class McpAppCatalog {
    public record Tool(String name, String title, Map<String, Object> inputSchema,
                       String resourceUri, Set<String> visibility, boolean readOnly) {
        public Tool { inputSchema = Map.copyOf(inputSchema); visibility = Set.copyOf(visibility); }
        public boolean modelVisible() { return visibility.contains("model"); }
    }
    private final Map<String, Map<String, Tool>> servers = new ConcurrentHashMap<>();
    private final Map<String, McpAppTransport> transports = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public Tool register(String server, String name, String title, Map<String, Object> schema,
                         Map<String, Object> meta, boolean readOnly) {
        Map<String, Object> ui = meta != null && meta.get("ui") instanceof Map<?, ?> m
                ? (Map<String, Object>) m : Map.of();
        String uri = ui.get("resourceUri") instanceof String s ? s : null;
        if (uri != null) {
            URI parsed = URI.create(uri);
            if (!"ui".equals(parsed.getScheme()) || uri.length() > 512 || parsed.getFragment() != null)
                throw new McpAppException("APP_RESOURCE_INVALID");
        }
        Set<String> visibility = new HashSet<>(Set.of("model", "app"));
        if (ui.containsKey("visibility")) {
            if (!(ui.get("visibility") instanceof List<?> list) || list.isEmpty()
                    || list.stream().anyMatch(v -> !Set.of("model", "app").contains(v)))
                throw new McpAppException("APP_RESOURCE_INVALID");
            visibility = new HashSet<>((List<String>) list);
        }
        Tool tool = new Tool(name, title == null ? name : title, schema, uri, visibility, readOnly);
        servers.computeIfAbsent(server, ignored -> new ConcurrentHashMap<>()).put(name, tool);
        return tool;
    }
    public void transport(String server, McpAppTransport transport) { transports.put(server, transport); }
    public McpAppTransport transport(String server) {
        var transport = transports.get(server);
        if (transport == null) throw new McpAppException("APP_NOT_ALLOWED");
        return transport;
    }
    public Tool required(String server, String tool) {
        Tool found = servers.getOrDefault(server, Map.of()).get(tool);
        if (found == null) throw new McpAppException("APP_TOOL_DENIED");
        return found;
    }
    public Collection<Tool> tools(String server) { return List.copyOf(servers.getOrDefault(server, Map.of()).values()); }
    public void remove(String server) { servers.remove(server); transports.remove(server); }
}
