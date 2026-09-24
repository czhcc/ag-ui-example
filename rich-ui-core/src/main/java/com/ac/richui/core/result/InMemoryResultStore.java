package com.ac.richui.core.result;

import com.ac.mcp.contract.result.McpResult;
import com.ac.richui.core.context.AccessSubject;
import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.tool.ToolIdentity;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Bounded, TTL-aware store intended for local development and tests. */
public final class InMemoryResultStore implements ResultStore {
    private final ConcurrentHashMap<ResultReference, StoredMcpResult> results = new ConcurrentHashMap<>();
    private final ResultStoreLimits limits;
    private final ResultReferenceGenerator references;
    private final Clock clock;
    private final ResultSizeEstimator sizes;

    public InMemoryResultStore(Duration ttl, int maximumEntries) {
        this(defaultLimits(ttl, maximumEntries), new UuidResultReferenceGenerator(),
                Clock.systemUTC(), new ConservativeResultSizeEstimator());
    }

    public InMemoryResultStore(ResultStoreLimits limits) {
        this(limits, new UuidResultReferenceGenerator(),
                Clock.systemUTC(), new ConservativeResultSizeEstimator());
    }

    public InMemoryResultStore(Duration ttl, int maximumEntries,
                               ResultReferenceGenerator references, Clock clock) {
        this(defaultLimits(ttl, maximumEntries), references, clock, new ConservativeResultSizeEstimator());
    }

    public InMemoryResultStore(ResultStoreLimits limits, ResultReferenceGenerator references,
                               Clock clock, ResultSizeEstimator sizes) {
        this.limits = Objects.requireNonNull(limits, "limits must not be null");
        this.references = Objects.requireNonNull(references, "references must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.sizes = Objects.requireNonNull(sizes, "sizes must not be null");
    }

    @Override
    public synchronized ResultReference save(RunScope scope, ToolIdentity tool, McpResult<?> result) {
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(tool, "tool must not be null");
        Objects.requireNonNull(result, "result must not be null");
        evictExpired();
        long estimatedBytes = sizes.estimateBytes(result, limits.maximumBytesPerResult());
        if (estimatedBytes < 1 || estimatedBytes > limits.maximumBytesPerResult()) {
            throw new ResultTooLargeException();
        }
        ensureTenantQuota(scope.tenantId(), estimatedBytes);
        ensureCapacity();
        ResultReference reference = references.next();
        Instant now = clock.instant();
        StoredMcpResult previous = results.putIfAbsent(reference,
                new StoredMcpResult(reference, scope, tool, result, estimatedBytes,
                        now, now.plus(limits.ttl())));
        if (previous != null) {
            throw new IllegalStateException("Result reference generator produced a duplicate");
        }
        return reference;
    }

    @Override
    public synchronized StoredMcpResult get(ResultReference reference, RunScope scope, AccessSubject subject) {
        Objects.requireNonNull(reference, "reference must not be null");
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(subject, "subject must not be null");
        StoredMcpResult stored = results.get(reference);
        if (stored == null) {
            throw new ResultNotFoundException();
        }
        if (!subject.owns(stored.scope()) || !sameRun(stored.scope(), scope)) {
            throw new ResultAccessDeniedException();
        }
        if (!clock.instant().isBefore(stored.expiresAt())) {
            results.remove(reference, stored);
            throw new ResultExpiredException();
        }
        return stored;
    }

    @Override
    public synchronized void remove(ResultReference reference, RunScope scope, AccessSubject subject) {
        get(reference, scope, subject);
        results.remove(reference);
    }

    private boolean sameRun(RunScope owner, RunScope request) {
        return owner.tenantId().equals(request.tenantId())
                && owner.userId().equals(request.userId())
                && owner.threadId().equals(request.threadId())
                && owner.runId().equals(request.runId());
    }

    private void evictExpired() {
        Instant now = clock.instant();
        results.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt()));
    }

    private void ensureCapacity() {
        while (results.size() >= limits.maximumEntries()) {
            results.entrySet().stream()
                    .min(Comparator.comparing(entry -> entry.getValue().createdAt()))
                    .ifPresent(entry -> results.remove(entry.getKey(), entry.getValue()));
        }
    }

    private void ensureTenantQuota(String tenantId, long incomingBytes) {
        int entries = 0;
        long bytes = 0;
        for (StoredMcpResult stored : results.values()) {
            if (!stored.scope().tenantId().equals(tenantId)) continue;
            entries++;
            bytes = Math.addExact(bytes, stored.estimatedBytes());
        }
        if (entries >= limits.maximumEntriesPerTenant()
                || bytes > limits.maximumBytesPerTenant() - incomingBytes) {
            throw new TenantQuotaExceededException();
        }
    }

    private static ResultStoreLimits defaultLimits(Duration ttl, int maximumEntries) {
        long perResult = 2L * 1024 * 1024;
        long perTenant = maximumEntries > Long.MAX_VALUE / perResult
                ? Long.MAX_VALUE : maximumEntries * perResult;
        return new ResultStoreLimits(ttl, maximumEntries, maximumEntries, perResult, perTenant);
    }
}
