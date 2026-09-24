package com.ac.richui.core.context;

import java.util.Objects;

/**
 * 一次 Agent 运行或工具调用的可信归属与关联信息；运行级操作可不含 toolCallId。
 */
public record RunScope(
        String tenantId,
        String userId,
        String threadId,
        String runId,
        String toolCallId) {

    /**
     * 校验各级标识，并规范化可选的工具调用标识。
     */
    public RunScope {
        tenantId = requireText(tenantId, "tenantId", 128);
        userId = requireText(userId, "userId", 128);
        threadId = requireText(threadId, "threadId", 200);
        runId = requireText(runId, "runId", 200);
        toolCallId = normalizeOptional(toolCallId);
    }

    /**
     * 返回带指定工具调用标识的新运行范围。
     */
    public RunScope withToolCallId(String newToolCallId) {
        return new RunScope(tenantId, userId, threadId, runId,
                requireText(newToolCallId, "toolCallId", 200));
    }

    /**
     * 判断当前范围是否包含工具调用。
     */
    public boolean hasToolCall() {
        return toolCallId != null;
    }

    private static String requireText(String value, String name, int maximumLength) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank() || value.length() > maximumLength) {
            throw new IllegalArgumentException(name + " must contain 1 to " + maximumLength + " characters");
        }
        return value;
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : requireText(value, "toolCallId", 200);
    }
}
