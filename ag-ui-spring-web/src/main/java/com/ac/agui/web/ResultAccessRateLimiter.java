package com.ac.agui.web;

import com.ac.richui.core.context.AccessSubject;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Per-subject fixed-window limiter with bounded identity tracking. */
public final class ResultAccessRateLimiter {
    private final ConcurrentHashMap<SubjectKey, Window> windows = new ConcurrentHashMap<>();
    private final int maximumRequests;
    private final long windowMillis;
    private final int maximumSubjects;
    private final Clock clock;

    public ResultAccessRateLimiter(int maximumRequests, Duration window, int maximumSubjects) {
        this(maximumRequests, window, maximumSubjects, Clock.systemUTC());
    }

    ResultAccessRateLimiter(int maximumRequests, Duration window, int maximumSubjects, Clock clock) {
        if (maximumRequests < 1) throw new IllegalArgumentException("maximumRequests must be positive");
        Objects.requireNonNull(window, "window must not be null");
        if (window.isZero() || window.isNegative()) throw new IllegalArgumentException("window must be positive");
        if (maximumSubjects < 1) throw new IllegalArgumentException("maximumSubjects must be positive");
        this.maximumRequests = maximumRequests;
        this.windowMillis = window.toMillis();
        if (windowMillis < 1) throw new IllegalArgumentException("window must be at least one millisecond");
        this.maximumSubjects = maximumSubjects;
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public Decision acquire(AccessSubject subject) {
        long now = clock.millis();
        SubjectKey key = new SubjectKey(subject.tenantId(), subject.userId());
        if (windows.size() >= maximumSubjects) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().startedAtMillis >= windowMillis);
            if (windows.size() >= maximumSubjects && !windows.containsKey(key)) {
                return new Decision(false, maximumRequests, Math.max(1, (windowMillis + 999) / 1_000));
            }
        }
        Decision[] decision = new Decision[1];
        windows.compute(key, (ignored, current) -> {
            Window active = current == null || now - current.startedAtMillis >= windowMillis
                    ? new Window(now, 0) : current;
            if (active.count >= maximumRequests) {
                long retryAfter = Math.max(1, (windowMillis - (now - active.startedAtMillis) + 999) / 1_000);
                decision[0] = new Decision(false, maximumRequests, retryAfter);
                return active;
            }
            decision[0] = new Decision(true, maximumRequests, 0);
            return new Window(active.startedAtMillis, active.count + 1);
        });
        return decision[0];
    }

    public record Decision(boolean allowed, int limit, long retryAfterSeconds) { }
    private record SubjectKey(String tenantId, String userId) { }
    private record Window(long startedAtMillis, int count) { }
}
