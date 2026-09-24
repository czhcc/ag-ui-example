package com.ac.richui.core.context;

import java.util.Objects;

/**
 * Trusted ownership and correlation data for one agent run or tool call.
 * {@code toolCallId} is absent for run-level operations.
 */
public record RunScope(
        String tenantId,
        String userId,
        String threadId,
        String runId,
        String toolCallId) {

    public RunScope {
        tenantId = requireText(tenantId, "tenantId", 128);
        userId = requireText(userId, "userId", 128);
        threadId = requireText(threadId, "threadId", 200);
        runId = requireText(runId, "runId", 200);
        toolCallId = normalizeOptional(toolCallId);
    }

    public RunScope withToolCallId(String newToolCallId) {
        return new RunScope(tenantId, userId, threadId, runId,
                requireText(newToolCallId, "toolCallId", 200));
    }

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
