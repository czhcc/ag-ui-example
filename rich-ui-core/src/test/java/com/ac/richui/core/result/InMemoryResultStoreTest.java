package com.ac.richui.core.result;

import com.ac.mcp.contract.presentation.PresentationHint;
import com.ac.mcp.contract.result.McpResult;
import com.ac.richui.core.context.AccessSubject;
import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.tool.ToolIdentity;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryResultStoreTest {
    private final RunScope scope = new RunScope("tenant-a", "user-a", "thread-a", "run-a", "call-a");
    private final ToolIdentity tool = new ToolIdentity("crm", "search");
    private final McpResult<?> result = McpResult.success(
            Map.of("name", "Alice"), null, PresentationHint.none(), null);

    @Test
    void requiresSubjectAndExactRunOwnership() {
        var store = new InMemoryResultStore(Duration.ofMinutes(5), 10);
        ResultReference reference = store.save(scope, tool, result);

        assertEquals(result, store.get(reference, scope, AccessSubject.of("tenant-a", "user-a")).result());
        assertThrows(ResultAccessDeniedException.class, () -> store.get(
                reference, scope, AccessSubject.of("tenant-a", "user-b")));
        assertThrows(ResultAccessDeniedException.class, () -> store.get(
                reference,
                new RunScope("tenant-a", "user-a", "thread-b", "run-a", null),
                AccessSubject.of("tenant-a", "user-a")));
        StoredMcpResult stored = store.get(reference, scope, AccessSubject.of("tenant-a", "user-a"));
        assertEquals("tenant-a", stored.scope().tenantId());
        assertEquals("user-a", stored.scope().userId());
        assertEquals("thread-a", stored.scope().threadId());
        assertEquals("run-a", stored.scope().runId());
        assertEquals("call-a", stored.scope().toolCallId());
    }

    @Test
    void removesExpiredValues() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        AtomicInteger sequence = new AtomicInteger();
        var store = new InMemoryResultStore(Duration.ofSeconds(5), 10,
                () -> new ResultReference("result_" + sequence.incrementAndGet()), clock);
        ResultReference reference = store.save(scope, tool, result);
        clock.advance(Duration.ofSeconds(5));

        assertThrows(ResultExpiredException.class, () -> store.get(
                reference, scope, AccessSubject.of("tenant-a", "user-a")));
        assertThrows(ResultNotFoundException.class, () -> store.get(
                reference, scope, AccessSubject.of("tenant-a", "user-a")));
    }

    @Test
    void enforcesSingleResultSizeAndTenantQuotas() {
        AtomicInteger sequence = new AtomicInteger();
        ResultStoreLimits limits = new ResultStoreLimits(Duration.ofMinutes(5), 10, 1, 10, 10);
        var quotaStore = new InMemoryResultStore(limits,
                () -> new ResultReference("quota_" + sequence.incrementAndGet()), Clock.systemUTC(),
                (ignored, stopAfter) -> 6);
        quotaStore.save(scope, tool, result);
        assertThrows(TenantQuotaExceededException.class, () -> quotaStore.save(
                new RunScope("tenant-a", "user-b", "thread-b", "run-b", "call-b"), tool, result));

        var sizeStore = new InMemoryResultStore(limits,
                () -> new ResultReference("large"), Clock.systemUTC(),
                (ignored, stopAfter) -> 11);
        assertThrows(ResultTooLargeException.class, () -> sizeStore.save(scope, tool, result));
    }

    @Test
    void evictsOldestEntryAtGlobalCapacity() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        AtomicInteger sequence = new AtomicInteger();
        ResultStoreLimits limits = new ResultStoreLimits(Duration.ofMinutes(5), 2, 2, 100, 200);
        var store = new InMemoryResultStore(limits,
                () -> new ResultReference("capacity_" + sequence.incrementAndGet()), clock,
                (ignored, stopAfter) -> 10);
        ResultReference oldest = store.save(scope, tool, result);
        clock.advance(Duration.ofMillis(1));
        store.save(new RunScope("tenant-b", "user", "thread", "run", "call"), tool, result);
        clock.advance(Duration.ofMillis(1));
        store.save(new RunScope("tenant-c", "user", "thread", "run", "call"), tool, result);

        assertThrows(ResultNotFoundException.class, () -> store.get(
                oldest, scope, AccessSubject.of("tenant-a", "user-a")));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
