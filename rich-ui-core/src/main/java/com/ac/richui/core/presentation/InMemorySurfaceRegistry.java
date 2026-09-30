package com.ac.richui.core.presentation;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

/** Bounded, expiring registry for a single application instance. */
public final class InMemorySurfaceRegistry implements SurfaceRegistry {
    private record Stored(Entry entry, Instant expiresAt) { }

    private final ConcurrentHashMap<String, Stored> entries = new ConcurrentHashMap<>();
    private final Duration ttl;
    private final int maximumEntries;
    private final Clock clock;

    public InMemorySurfaceRegistry(Duration ttl, int maximumEntries) {
        this(ttl, maximumEntries, Clock.systemUTC());
    }

    public InMemorySurfaceRegistry(Duration ttl, int maximumEntries, Clock clock) {
        if (ttl == null || ttl.isNegative() || ttl.isZero()) throw new IllegalArgumentException("ttl must be positive");
        if (maximumEntries < 1) throw new IllegalArgumentException("maximumEntries must be positive");
        this.ttl = ttl;
        this.maximumEntries = maximumEntries;
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public synchronized void register(Entry entry) {
        Objects.requireNonNull(entry, "entry must not be null");
        evictExpired();
        if (entries.size() >= maximumEntries) throw new IllegalStateException("Surface registry capacity exceeded");
        if (entries.putIfAbsent(entry.spec().surfaceId(), new Stored(entry, clock.instant().plus(ttl))) != null)
            throw new IllegalArgumentException("Duplicate surfaceId");
    }

    @Override
    public Entry replace(String surfaceId, UnaryOperator<Entry> change) {
        Objects.requireNonNull(surfaceId, "surfaceId must not be null");
        Objects.requireNonNull(change, "change must not be null");
        Stored updated = entries.compute(surfaceId, (id, existing) -> {
            if (existing == null || !clock.instant().isBefore(existing.expiresAt()))
                throw new IllegalArgumentException("Unknown or expired surfaceId");
            Entry replacement = Objects.requireNonNull(change.apply(existing.entry()), "replacement must not be null");
            if (!replacement.owner().equals(existing.entry().owner())
                    || !replacement.spec().surfaceId().equals(id))
                throw new IllegalArgumentException("Surface owner and ID cannot change");
            return new Stored(replacement, existing.expiresAt());
        });
        return updated.entry();
    }

    private void evictExpired() {
        Instant now = clock.instant();
        entries.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt()));
    }
}
