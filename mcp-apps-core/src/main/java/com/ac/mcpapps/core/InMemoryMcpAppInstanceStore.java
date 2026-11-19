package com.ac.mcpapps.core;

import java.time.Clock;
import java.util.*;
import java.util.function.Function;

/** Bounded local-development store; production multi-replica startup refuses this implementation. */
public final class InMemoryMcpAppInstanceStore implements McpAppInstanceStore {
    private final Map<String, McpAppInstance> entries = new java.util.concurrent.ConcurrentHashMap<>();
    private final Clock clock;
    public InMemoryMcpAppInstanceStore(Clock clock) { this.clock = clock; }
    @Override public synchronized void put(McpAppInstance instance) {
        entries.values().removeIf(v -> !v.expiresAt.isAfter(clock.instant()));
        if (entries.size() >= 1000 || entries.values().stream()
                .filter(v -> v.origin.tenantId().equals(instance.origin.tenantId())).count() >= 100)
            throw new McpAppException("APP_QUOTA_EXCEEDED");
        entries.put(instance.id, instance);
    }
    @Override public <T> T update(String id, Function<McpAppInstance, T> operation) {
        var value = entries.get(id);
        if (value == null) throw new McpAppException("APP_NOT_FOUND");
        synchronized (value) { return operation.apply(value); }
    }
}
