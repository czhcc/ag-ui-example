package com.ac.richui.core.result;

import java.time.Duration;
import java.util.Objects;

/** Hard storage boundaries applied before a full MCP result is retained. */
public record ResultStoreLimits(
        Duration ttl,
        int maximumEntries,
        int maximumEntriesPerTenant,
        long maximumBytesPerResult,
        long maximumBytesPerTenant) {

    public ResultStoreLimits {
        Objects.requireNonNull(ttl, "ttl must not be null");
        if (ttl.isZero() || ttl.isNegative()) throw new IllegalArgumentException("ttl must be positive");
        if (maximumEntries < 1) throw new IllegalArgumentException("maximumEntries must be positive");
        if (maximumEntriesPerTenant < 1 || maximumEntriesPerTenant > maximumEntries) {
            throw new IllegalArgumentException("maximumEntriesPerTenant must be between 1 and maximumEntries");
        }
        if (maximumBytesPerResult < 1) {
            throw new IllegalArgumentException("maximumBytesPerResult must be positive");
        }
        if (maximumBytesPerTenant < maximumBytesPerResult) {
            throw new IllegalArgumentException("maximumBytesPerTenant must allow at least one result");
        }
    }
}
