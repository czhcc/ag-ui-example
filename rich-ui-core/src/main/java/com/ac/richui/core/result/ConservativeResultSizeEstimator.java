package com.ac.richui.core.result;

import com.ac.mcp.contract.result.McpResult;
import java.lang.reflect.Array;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.time.temporal.TemporalAccessor;
import java.util.IdentityHashMap;
import java.util.Map;

/** Cycle-safe UTF-8 estimate suitable for enforcing an upper storage boundary. */
public final class ConservativeResultSizeEstimator implements ResultSizeEstimator {
    @Override
    public long estimateBytes(McpResult<?> result, long stopAfterBytes) {
        return estimate(result, stopAfterBytes, new IdentityHashMap<>(), 0);
    }

    private long estimate(Object value, long limit, IdentityHashMap<Object, Boolean> seen, int depth) {
        if (value == null) return 4;
        if (depth > 32) return limit + 1;
        if (value instanceof CharSequence text) return utf8(text.toString()) + 2;
        if (value instanceof Number || value instanceof Boolean || value instanceof Enum<?>
                || value instanceof TemporalAccessor) return utf8(String.valueOf(value));
        if (seen.put(value, Boolean.TRUE) != null) return 0;
        long size = 2;
        try {
            if (value instanceof Map<?, ?> map) {
                for (var entry : map.entrySet()) {
                    size = add(size, estimate(String.valueOf(entry.getKey()), limit, seen, depth + 1), limit);
                    size = add(size, estimate(entry.getValue(), limit, seen, depth + 1), limit);
                    if (size > limit) return size;
                }
                return size;
            }
            if (value instanceof Iterable<?> iterable) {
                for (Object item : iterable) {
                    size = add(size, estimate(item, limit, seen, depth + 1), limit);
                    if (size > limit) return size;
                }
                return size;
            }
            if (value.getClass().isArray()) {
                for (int index = 0; index < Array.getLength(value); index++) {
                    size = add(size, estimate(Array.get(value, index), limit, seen, depth + 1), limit);
                    if (size > limit) return size;
                }
                return size;
            }
            if (value.getClass().isRecord()) {
                for (RecordComponent component : value.getClass().getRecordComponents()) {
                    try {
                        size = add(size, estimate(component.getAccessor().invoke(value), limit, seen, depth + 1), limit);
                    } catch (ReflectiveOperationException exception) {
                        return limit + 1;
                    }
                    if (size > limit) return size;
                }
                return size;
            }
            return add(size, utf8(String.valueOf(value)), limit);
        } finally {
            seen.remove(value);
        }
    }

    private long add(long left, long right, long limit) {
        if (left > limit || right > limit || Long.MAX_VALUE - left < right) return limit + 1;
        return left + right;
    }

    private long utf8(String value) { return value.getBytes(StandardCharsets.UTF_8).length; }
}
