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

/**
 * 用于本地开发和测试的有界内存结果存储，支持过期与租户配额。
 */
public final class InMemoryResultStore implements ResultStore {
    private final ConcurrentHashMap<ResultReference, StoredMcpResult> results = new ConcurrentHashMap<>();
    private final ResultStoreLimits limits;
    private final ResultReferenceGenerator references;
    private final Clock clock;
    private final ResultSizeEstimator sizes;

    /**
     * 以生存时间和全局条目上限创建存储。
     */
    public InMemoryResultStore(Duration ttl, int maximumEntries) {
        this(defaultLimits(ttl, maximumEntries), new UuidResultReferenceGenerator(),
                Clock.systemUTC(), new ConservativeResultSizeEstimator());
    }

    /**
     * 使用指定限制和默认基础组件创建存储。
     */
    public InMemoryResultStore(ResultStoreLimits limits) {
        this(limits, new UuidResultReferenceGenerator(),
                Clock.systemUTC(), new ConservativeResultSizeEstimator());
    }

    /**
     * 使用指定引用生成器和时钟创建存储。
     */
    public InMemoryResultStore(Duration ttl, int maximumEntries,
                               ResultReferenceGenerator references, Clock clock) {
        this(defaultLimits(ttl, maximumEntries), references, clock, new ConservativeResultSizeEstimator());
    }

    /**
     * 使用完整的限制、引用生成器、时钟和大小估算器创建存储。
     */
    public InMemoryResultStore(ResultStoreLimits limits, ResultReferenceGenerator references,
                               Clock clock, ResultSizeEstimator sizes) {
        this.limits = Objects.requireNonNull(limits, "limits must not be null");
        this.references = Objects.requireNonNull(references, "references must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.sizes = Objects.requireNonNull(sizes, "sizes must not be null");
    }

    /**
     * 校验大小与配额后保存结果，并返回结果引用。
     */
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

    /**
     * 校验访问主体、运行范围和有效期后读取结果。
     */
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

    /**
     * 在授权校验通过后删除结果。
     */
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
