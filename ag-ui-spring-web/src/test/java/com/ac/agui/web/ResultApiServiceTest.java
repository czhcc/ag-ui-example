package com.ac.agui.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.ac.agui.protocol.AgUiProtocolException;
import com.ac.mcp.contract.presentation.PresentationHint;
import com.ac.mcp.contract.result.McpResult;
import com.ac.richui.core.context.AccessSubject;
import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.result.InMemoryResultStore;
import com.ac.richui.core.result.ResultReference;
import com.ac.richui.core.result.ResultStoreLimits;
import com.ac.richui.core.tool.ToolIdentity;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

/** 验证结果读取的授权、限流、错误响应和审计行为。 */
class ResultApiServiceTest {
    private static final RunScope OWNER =
            new RunScope("tenant", "owner", "thread", "run", "call");
    private static final McpResult<?> RESULT = McpResult.success(
            Map.of("secret", "full result stays only in the response"), null,
            PresentationHint.none(), null);

    @Test
    void unauthenticatedResponsesAreAuditedAndNeverCached() {
        List<ResultAccessAuditEvent> audits = new ArrayList<>();
        var store = new InMemoryResultStore(Duration.ofMinutes(5), 10);
        var service = service(store, ignored -> {
            throw new AgUiProtocolException("UNAUTHENTICATED", "missing identity");
        }, audits, 10);

        var response = service.get("result", "thread", "run", request());
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("private, no-store, max-age=0",
                response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL));
        assertEquals(ResultAccessAuditEvent.Outcome.DENIED, audits.get(0).outcome());
    }

    @Test
    void distinguishesObjectAuthorizationNotFoundAndSuccessWithoutCaching() {
        AtomicReference<AccessSubject> caller = new AtomicReference<>(AccessSubject.of("tenant", "owner"));
        List<ResultAccessAuditEvent> audits = new ArrayList<>();
        var store = new InMemoryResultStore(Duration.ofMinutes(5), 10,
                () -> new ResultReference("result-1"), Clock.systemUTC());
        store.save(OWNER, new ToolIdentity("knowledge", "search"), RESULT);
        var service = service(store, request -> caller.get(), audits, 100);

        var allowed = service.get("result-1", "thread", "run", request());
        assertEquals(HttpStatus.OK, allowed.getStatusCode());
        assertEquals("private, no-store, max-age=0", allowed.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL));
        assertInstanceOf(ResultApiService.ResultPayload.class, allowed.getBody());

        caller.set(AccessSubject.of("tenant", "intruder"));
        assertEquals(HttpStatus.FORBIDDEN,
                service.get("result-1", "thread", "run", request()).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND,
                service.get("missing", "thread", "run", request()).getStatusCode());
        assertEquals(List.of(
                ResultAccessAuditEvent.Outcome.ALLOWED,
                ResultAccessAuditEvent.Outcome.DENIED,
                ResultAccessAuditEvent.Outcome.NOT_FOUND),
                audits.stream().map(ResultAccessAuditEvent::outcome).toList());
    }

    @Test
    void returnsGoneForExpiredResultsAndLimitsEachAuthenticatedSubject() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        var store = new InMemoryResultStore(
                new ResultStoreLimits(Duration.ofSeconds(1), 10, 10, 10_000, 100_000),
                () -> new ResultReference("expired"), clock, (ignored, stopAfter) -> 100);
        store.save(OWNER, new ToolIdentity("knowledge", "search"), RESULT);
        clock.advance(Duration.ofSeconds(1));
        var expiredIntruder = service(store, ignored -> AccessSubject.of("tenant", "intruder"),
                new ArrayList<>(), 10);
        assertEquals(HttpStatus.FORBIDDEN,
                expiredIntruder.get("expired", "thread", "run", request()).getStatusCode());
        var expired = service(store, ignored -> AccessSubject.of("tenant", "owner"), new ArrayList<>(), 10);
        assertEquals(HttpStatus.GONE, expired.get("expired", "thread", "run", request()).getStatusCode());

        var liveStore = new InMemoryResultStore(Duration.ofMinutes(5), 10,
                () -> new ResultReference("live"), Clock.systemUTC());
        liveStore.save(OWNER, new ToolIdentity("knowledge", "search"), RESULT);
        var limited = service(liveStore, ignored -> AccessSubject.of("tenant", "owner"),
                new ArrayList<>(), 1);
        assertEquals(HttpStatus.OK, limited.get("live", "thread", "run", request()).getStatusCode());
        var rejected = limited.get("live", "thread", "run", request());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, rejected.getStatusCode());
        assertEquals("60", rejected.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
    }

    private ResultApiService service(InMemoryResultStore store, AgUiSubjectResolver resolver,
                                     List<ResultAccessAuditEvent> audits, int limit) {
        return new ResultApiService(store, resolver,
                new ResultAccessRateLimiter(limit, Duration.ofMinutes(1), 100), audits::add);
    }

    private MockServerHttpRequest request() {
        return MockServerHttpRequest.get("/api/results/result-1").build();
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private MutableClock(Instant instant) { this.instant = instant; }
        private void advance(Duration duration) { instant = instant.plus(duration); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
