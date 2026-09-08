package com.ac.agent.mcp.result;

import com.ac.mcp.contract.result.McpResult;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.Instant;

@Component
public class InMemoryResultStore implements ResultStore {
    private static final Logger log = LoggerFactory.getLogger(InMemoryResultStore.class);
    private final Cache<String, StoredMcpResult> cache;
    private final Duration ttl;
    private final ResultRefGenerator generator;

    public InMemoryResultStore(@Value("${ac.result-store.ttl:45m}") Duration ttl, ResultRefGenerator generator) {
        this.ttl = ttl;
        this.generator = generator;
        this.cache = Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(10_000).build();
    }
    @Override public String save(String serverCode, String toolName, McpResult<?> result) {
        return save(serverCode, toolName, null, null, result);
    }
    @Override public String save(String serverCode, String toolName, String conversationId, String runId, McpResult<?> result) {
        var ref = generator.next();
        var now = Instant.now();
        cache.put(ref, new StoredMcpResult(ref, serverCode, toolName, conversationId, runId, result, now, now.plus(ttl)));
        log.info("Stored MCP result resultRef={} serverCode={} tool={}", ref, serverCode, toolName);
        return ref;
    }
    @Override public StoredMcpResult get(String resultRef) {
        var value = cache.getIfPresent(resultRef);
        if (value == null) throw new IllegalArgumentException("Unknown or expired resultRef: " + resultRef);
        return value;
    }
    @Override public void remove(String resultRef) { cache.invalidate(resultRef); }
}
