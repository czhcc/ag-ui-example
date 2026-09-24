package com.ac.richui.core.result;

import java.time.Duration;
import java.util.Objects;

/**
 * 保存完整 MCP 结果前应用的有效期、容量和租户配额限制。
 */
public record ResultStoreLimits(
        Duration ttl,
        int maximumEntries,
        int maximumEntriesPerTenant,
        long maximumBytesPerResult,
        long maximumBytesPerTenant) {

    /**
     * 校验有效期、条目数和字节配额的边界。
     */
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
