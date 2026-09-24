package com.ac.agui.web;

import com.ac.agui.protocol.AgUiProtocolException;
import com.ac.richui.core.context.AccessSubject;
import com.ac.richui.core.context.RunScope;
import com.ac.richui.core.result.ResultAccessDeniedException;
import com.ac.richui.core.result.ResultExpiredException;
import com.ac.richui.core.result.ResultNotFoundException;
import com.ac.richui.core.result.ResultReference;
import com.ac.richui.core.result.ResultStore;
import com.ac.richui.core.result.ResultStoreException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;

/** Shared secure implementation for both runtime-specific Result API controllers. */
public final class ResultApiService {
    private static final String CACHE_CONTROL = "private, no-store, max-age=0";
    private final ResultStore results;
    private final AgUiSubjectResolver subjects;
    private final ResultAccessRateLimiter rateLimiter;
    private final ResultAccessAuditor auditor;
    private final Clock clock;

    public ResultApiService(ResultStore results, AgUiSubjectResolver subjects,
                            ResultAccessRateLimiter rateLimiter, ResultAccessAuditor auditor) {
        this(results, subjects, rateLimiter, auditor, Clock.systemUTC());
    }

    ResultApiService(ResultStore results, AgUiSubjectResolver subjects,
                     ResultAccessRateLimiter rateLimiter, ResultAccessAuditor auditor, Clock clock) {
        this.results = Objects.requireNonNull(results, "results must not be null");
        this.subjects = Objects.requireNonNull(subjects, "subjects must not be null");
        this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter must not be null");
        this.auditor = Objects.requireNonNull(auditor, "auditor must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public ResponseEntity<?> get(String resultRef, String threadId, String runId, ServerHttpRequest request) {
        AccessSubject subject;
        try {
            subject = subjects.resolve(request);
        } catch (AgUiProtocolException exception) {
            auditor.record(new ResultAccessAuditEvent(Instant.now(clock), null, null,
                    threadId, runId, resultRef, ResultAccessAuditEvent.Outcome.DENIED, 401));
            return response(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorPayload("UNAUTHENTICATED", "Authentication is required"));
        }
        ResultAccessRateLimiter.Decision rate = rateLimiter.acquire(subject);
        if (!rate.allowed()) {
            audit(subject, threadId, runId, resultRef, ResultAccessAuditEvent.Outcome.RATE_LIMITED, 429);
            return response(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(rate.retryAfterSeconds()))
                    .header("X-RateLimit-Limit", String.valueOf(rate.limit()))
                    .body(new ErrorPayload("RESULT_RATE_LIMITED", "Too many result requests"));
        }
        try {
            RunScope scope = new RunScope(subject.tenantId(), subject.userId(), threadId, runId, null);
            var stored = results.get(new ResultReference(resultRef), scope, subject);
            audit(subject, threadId, runId, resultRef, ResultAccessAuditEvent.Outcome.ALLOWED, 200);
            return response(HttpStatus.OK).body(new ResultPayload(
                    stored.reference().value(), stored.tool().serverCode(), stored.tool().toolName(),
                    stored.result().data(), stored.result().summary(), stored.result().resultMeta(),
                    stored.expiresAt().toEpochMilli()));
        } catch (ResultAccessDeniedException exception) {
            return failure(subject, threadId, runId, resultRef, HttpStatus.FORBIDDEN,
                    "RESULT_FORBIDDEN", "Result does not belong to this caller",
                    ResultAccessAuditEvent.Outcome.DENIED);
        } catch (ResultExpiredException exception) {
            return failure(subject, threadId, runId, resultRef, HttpStatus.GONE,
                    "RESULT_EXPIRED", "Result has expired", ResultAccessAuditEvent.Outcome.EXPIRED);
        } catch (ResultNotFoundException exception) {
            return failure(subject, threadId, runId, resultRef, HttpStatus.NOT_FOUND,
                    "RESULT_NOT_FOUND", "Result was not found", ResultAccessAuditEvent.Outcome.NOT_FOUND);
        } catch (IllegalArgumentException exception) {
            return failure(subject, threadId, runId, resultRef, HttpStatus.BAD_REQUEST,
                    "INVALID_RESULT_REQUEST", "Invalid result request", ResultAccessAuditEvent.Outcome.INVALID);
        } catch (ResultStoreException exception) {
            return failure(subject, threadId, runId, resultRef, HttpStatus.SERVICE_UNAVAILABLE,
                    "RESULT_STORE_UNAVAILABLE", "Result store is unavailable", ResultAccessAuditEvent.Outcome.ERROR);
        }
    }

    private ResponseEntity<?> failure(AccessSubject subject, String threadId, String runId, String resultRef,
                                      HttpStatus status, String code, String message,
                                      ResultAccessAuditEvent.Outcome outcome) {
        audit(subject, threadId, runId, resultRef, outcome, status.value());
        return response(status).body(new ErrorPayload(code, message));
    }

    private ResponseEntity.BodyBuilder response(HttpStatus status) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL)
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header("X-Content-Type-Options", "nosniff");
    }

    private void audit(AccessSubject subject, String threadId, String runId, String resultRef,
                       ResultAccessAuditEvent.Outcome outcome, int status) {
        auditor.record(new ResultAccessAuditEvent(Instant.now(clock), subject.tenantId(), subject.userId(),
                threadId, runId, resultRef, outcome, status));
    }

    public record ResultPayload(String resultRef, String serverCode, String toolName,
                                Object data, Object summary, Object resultMeta, long expiresAtEpochMs) { }
    public record ErrorPayload(String code, String message) { }
}
