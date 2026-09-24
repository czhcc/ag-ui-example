package com.ac.runtime.saa;

import com.ac.richui.core.context.RunScope;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Stable mapping between project-owned run identity and SAA RunnableConfig. */
public final class SaaRunMetadata {
    public static final String TENANT_ID = "ac.tenantId";
    public static final String USER_ID = "ac.userId";
    public static final String THREAD_ID = "ac.threadId";
    public static final String RUN_ID = "ac.runId";
    public static final String TOOL_CALL_ID = "ac.toolCallId";

    private SaaRunMetadata() { }

    public static RunnableConfig config(RunScope scope) {
        Objects.requireNonNull(scope, "scope must not be null");
        RunnableConfig.Builder builder = RunnableConfig.builder()
                .threadId(scope.threadId())
                .addMetadata(TENANT_ID, scope.tenantId())
                .addMetadata(USER_ID, scope.userId())
                .addMetadata(THREAD_ID, scope.threadId())
                .addMetadata(RUN_ID, scope.runId());
        if (scope.toolCallId() != null) {
            builder.addMetadata(TOOL_CALL_ID, scope.toolCallId());
        }
        return builder.build();
    }

    public static RunScope requireScope(RunnableConfig config, String toolCallId) {
        Objects.requireNonNull(config, "config must not be null");
        return new RunScope(
                required(config, TENANT_ID),
                required(config, USER_ID),
                required(config, THREAD_ID),
                required(config, RUN_ID),
                toolCallId);
    }

    public static RunScope requireScope(Map<String, Object> context) {
        Objects.requireNonNull(context, "context must not be null");
        return new RunScope(
                required(context, TENANT_ID),
                required(context, USER_ID),
                required(context, THREAD_ID),
                required(context, RUN_ID),
                optional(context, TOOL_CALL_ID));
    }

    public static Map<String, Object> toolContext(RunScope scope) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(TENANT_ID, scope.tenantId());
        values.put(USER_ID, scope.userId());
        values.put(THREAD_ID, scope.threadId());
        values.put(RUN_ID, scope.runId());
        if (scope.toolCallId() != null) {
            values.put(TOOL_CALL_ID, scope.toolCallId());
        }
        return values;
    }

    private static String required(RunnableConfig config, String key) {
        return config.metadata(key).map(String::valueOf)
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new IllegalStateException("Missing trusted run metadata: " + key));
    }

    private static String required(Map<String, Object> values, String key) {
        String value = optional(values, key);
        if (value == null) {
            throw new IllegalStateException("Missing trusted tool context: " + key);
        }
        return value;
    }

    private static String optional(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value);
    }
}
