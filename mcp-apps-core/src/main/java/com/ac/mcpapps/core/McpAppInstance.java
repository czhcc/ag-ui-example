package com.ac.mcpapps.core;

import com.ac.richui.core.context.RunScope;
import java.time.Instant;
import java.util.*;

/** Access only within the store's atomic update. Tokens and DOM are never part of this state. */
public final class McpAppInstance {
    public final String id;
    public final RunScope origin;
    public final String server;
    public final String tool;
    public final String uri;
    public final String hash;
    public final String title;
    public final Instant expiresAt;
    public final Set<String> grantedTools;
    public final Map<String, Object> initialResult;
    public final String initialRef;
    public String latestRef;
    public RunScope latestScope;
    public Map<String, Object> latestResult;
    public long revision = 1;
    public String status = "ACTIVE";
    public Map<String, Object> context = Map.of();
    public final Map<String, CachedCall> requests = new LinkedHashMap<>();
    public final Map<String, Followup> followups = new HashMap<>();
    public record CachedCall(String fingerprint, Map<String, Object> response) {}
    public record Followup(Instant expiresAt, String question, Map<String, Object> context, long revision) {}
    public McpAppInstance(String id, RunScope origin, String server, String tool, String uri, String hash,
                          String title, Instant expiresAt, Set<String> grantedTools,
                          Map<String, Object> initialResult, String initialRef) {
        this.id = id; this.origin = origin; this.server = server; this.tool = tool; this.uri = uri;
        this.hash = hash; this.title = title; this.expiresAt = expiresAt; this.grantedTools = Set.copyOf(grantedTools);
        this.initialResult = initialResult; this.initialRef = initialRef;
        this.latestRef = initialRef; this.latestScope = origin; this.latestResult = initialResult;
    }
    public Map<String, Object> descriptor() {
        return Map.ofEntries(Map.entry("profile", "ac.mcp-app"), Map.entry("profileVersion", "1.0"),
                Map.entry("appInstanceId", id), Map.entry("messageId", origin.runId() + ":assistant"),
                Map.entry("threadId", origin.threadId()), Map.entry("originRunId", origin.runId()),
                Map.entry("toolCallId", origin.toolCallId()), Map.entry("title", title),
                Map.entry("resourceHash", hash), Map.entry("resultRef", latestRef), Map.entry("revision", revision));
    }
}
